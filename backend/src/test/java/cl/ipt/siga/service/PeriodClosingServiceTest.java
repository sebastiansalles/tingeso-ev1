package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cl.ipt.siga.entity.enums.PeriodStatus.CLOSED;
import static cl.ipt.siga.entity.enums.PeriodStatus.OPEN;
import static cl.ipt.siga.entity.enums.StudentStatus.REGULAR;
import static cl.ipt.siga.entity.enums.SubjectResult.APPROVED;
import static cl.ipt.siga.entity.enums.SubjectResult.FAILED;
import static cl.ipt.siga.service.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodClosingServiceTest {

    @Mock private AcademicPeriodService academicPeriodService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentService studentService;
    @Mock private SubjectService subjectService;

    @InjectMocks
    private PeriodClosingService periodClosingService;

    private static final Long PERIOD = 100L;         // the period being closed
    private static final Long OLD_PERIOD = 50L;      // already closed
    private static final Long STUDENT = 1L;

    private AcademicPeriodEntity givenOpenPeriod() {
        AcademicPeriodEntity period = period(PERIOD, 2027, 1, OPEN);
        when(academicPeriodService.requireOpen(PERIOD)).thenReturn(period);
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(PERIOD));
        return period;
    }

    @Test
    void whenGradesAreMissing_thenRejectedListingSections() {
        // Given
        when(academicPeriodService.requireOpen(PERIOD)).thenReturn(period(PERIOD, 2027, 1, OPEN));
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, PERIOD, 5.0, APPROVED),
                enrollment(2L, STUDENT, 2L, 12L, PERIOD, null, null),
                enrollment(3L, 2L, 2L, 12L, PERIOD, null, null)));

        // When / Then: SUB2 appears once, even if two students lack a grade there
        assertThatThrownBy(() -> periodClosingService.close(PERIOD))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SUB2")
                .hasMessageNotContaining("SUB1");
        verify(academicPeriodService, never()).markAsClosed(any());
    }

    @Test
    void whenPeriodIsAlreadyClosed_thenRejected() {
        when(academicPeriodService.requireOpen(PERIOD)).thenThrow(new BusinessRuleException("closed"));

        assertThatThrownBy(() -> periodClosingService.close(PERIOD)).isInstanceOf(BusinessRuleException.class);
        verify(academicPeriodService, never()).markAsClosed(any());
    }

    @Test
    void whenPeriodHasNoEnrollments_thenClosed() {
        AcademicPeriodEntity period = givenOpenPeriod();
        when(academicPeriodService.markAsClosed(period)).thenAnswer(invocation -> {
            period.setStatus(CLOSED);
            return period;
        });

        assertThat(periodClosingService.close(PERIOD).getStatus()).isEqualTo(CLOSED);
    }

    @Test
    void whenStudentFailsSameSubjectSecondTime_thenEliminated() {
        // Given: subject 1 failed in an old closed period and again now
        AcademicPeriodEntity period = givenOpenPeriod();
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD))
                .thenReturn(List.of(enrollment(2L, STUDENT, 1L, 11L, PERIOD, 3.0, FAILED)));
        when(enrollmentRepository.findByStudentId(STUDENT)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, OLD_PERIOD, 2.5, FAILED),
                enrollment(2L, STUDENT, 1L, 11L, PERIOD, 3.0, FAILED)));

        // When
        periodClosingService.close(PERIOD);

        // Then
        verify(studentService).markAsEliminated(STUDENT);
        verify(studentService, never()).markAsGraduated(any());
        verify(academicPeriodService).markAsClosed(period);
    }

    @Test
    void whenFailureIsInAnotherOpenPeriod_thenItDoesNotCount() {
        // Given: the other failure belongs to a period that is still open
        when(academicPeriodService.requireOpen(PERIOD)).thenReturn(period(PERIOD, 2027, 1, OPEN));
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(PERIOD, 200L));
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD))
                .thenReturn(List.of(enrollment(2L, STUDENT, 1L, 11L, PERIOD, 3.0, FAILED)));
        when(enrollmentRepository.findByStudentId(STUDENT)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, 200L, 2.5, FAILED),
                enrollment(2L, STUDENT, 1L, 11L, PERIOD, 3.0, FAILED)));
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(subjectService.getByStudyPlan(10L)).thenReturn(List.of(subject(1L, 10L, "TAP101", 1)));

        // When
        periodClosingService.close(PERIOD);

        // Then
        verify(studentService, never()).markAsEliminated(any());
    }

    @Test
    void whenStudentApprovesWholePlan_thenGraduated() {
        // Given: a plan of 2 subjects, one approved before and one approved now
        givenOpenPeriod();
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD))
                .thenReturn(List.of(enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(enrollmentRepository.findByStudentId(STUDENT)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, OLD_PERIOD, 5.0, APPROVED),
                enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(subjectService.getByStudyPlan(10L)).thenReturn(List.of(
                subject(1L, 10L, "TAP101", 1), subject(2L, 10L, "TAP102", 1)));

        // When
        periodClosingService.close(PERIOD);

        // Then
        verify(studentService).markAsGraduated(STUDENT);
    }

    @Test
    void whenStudentStillHasPendingSubjects_thenStatusUnchanged() {
        givenOpenPeriod();
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD))
                .thenReturn(List.of(enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(enrollmentRepository.findByStudentId(STUDENT))
                .thenReturn(List.of(enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(subjectService.getByStudyPlan(10L)).thenReturn(List.of(
                subject(1L, 10L, "TAP101", 1), subject(2L, 10L, "TAP102", 1)));

        periodClosingService.close(PERIOD);

        verify(studentService, never()).markAsGraduated(any());
        verify(studentService, never()).markAsEliminated(any());
    }

    @Test
    void whenStudyPlanHasNoSubjects_thenNotGraduated() {
        givenOpenPeriod();
        when(enrollmentRepository.findByAcademicPeriodId(PERIOD))
                .thenReturn(List.of(enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(enrollmentRepository.findByStudentId(STUDENT))
                .thenReturn(List.of(enrollment(2L, STUDENT, 2L, 12L, PERIOD, 6.0, APPROVED)));
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(subjectService.getByStudyPlan(10L)).thenReturn(List.of());

        periodClosingService.close(PERIOD);

        verify(studentService, never()).markAsGraduated(any());
    }
}
