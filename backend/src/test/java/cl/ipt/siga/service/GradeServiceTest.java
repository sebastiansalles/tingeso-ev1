package cl.ipt.siga.service;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.service.TestData.student;
import static cl.ipt.siga.entity.enums.StudentStatus.REGULAR;
import static cl.ipt.siga.entity.enums.SubjectResult.APPROVED;
import static cl.ipt.siga.entity.enums.SubjectResult.FAILED;
import static cl.ipt.siga.service.TestData.enrollment;
import static cl.ipt.siga.service.TestData.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SectionService sectionService;

    @Mock
    private AcademicPeriodService academicPeriodService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private GradeService gradeService;

    private static final Long TEACHER = 7L;
    private static final Long PERIOD = 100L;

    // Enrollment 1: student 1 in section 11 (teacher 7), period 100, still without grade
    private EnrollmentEntity givenUngradedEnrollment() {
        EnrollmentEntity enrollment = enrollment(1L, 1L, 1L, 11L, PERIOD, null, null);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(sectionService.getById(11L)).thenReturn(section(11L, 1L, PERIOD, TEACHER, 40));
        return enrollment;
    }

    @Test
    void whenGetSectionEnrollments_thenChecksSectionAndReturnsStudents() {
        when(enrollmentRepository.findBySectionId(11L)).thenReturn(List.of(enrollment(1L, 1L, 1L, 11L, PERIOD, null, null)));

        assertThat(gradeService.getSectionEnrollments(11L)).hasSize(1);
        verify(sectionService).getById(11L);
    }

    @Test
    void whenGradeIsPassing_thenApproved() {
        // Given
        EnrollmentEntity enrollment = givenUngradedEnrollment();
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        // When
        EnrollmentEntity graded = gradeService.registerGrade(1L, TEACHER, 5.5);

        // Then
        assertThat(graded.getFinalGrade()).isEqualTo(5.5);
        assertThat(graded.getResult()).isEqualTo(APPROVED);
        verify(academicPeriodService).requireOpen(PERIOD);
    }

    @Test
    void whenGradeIsExactly4_thenApproved() {
        EnrollmentEntity enrollment = givenUngradedEnrollment();
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        assertThat(gradeService.registerGrade(1L, TEACHER, 4.0).getResult()).isEqualTo(APPROVED);
    }

    @Test
    void whenGradeIsBelow4_thenFailed() {
        EnrollmentEntity enrollment = givenUngradedEnrollment();
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        assertThat(gradeService.registerGrade(1L, TEACHER, 3.9).getResult()).isEqualTo(FAILED);
    }

    @Test
    void whenAnotherTeacherGrades_thenRejected() {
        givenUngradedEnrollment();

        assertThatThrownBy(() -> gradeService.registerGrade(1L, 99L, 5.0))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("responsible");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void whenGradeAbove7_thenRejected() {
        givenUngradedEnrollment();

        assertThatThrownBy(() -> gradeService.registerGrade(1L, TEACHER, 7.1))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void whenGradeBelow1_thenRejected() {
        givenUngradedEnrollment();

        assertThatThrownBy(() -> gradeService.registerGrade(1L, TEACHER, 0.9))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void whenGradeIsMissing_thenRejected() {
        givenUngradedEnrollment();

        assertThatThrownBy(() -> gradeService.registerGrade(1L, TEACHER, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void whenPeriodIsClosed_thenRejected() {
        givenUngradedEnrollment();
        when(academicPeriodService.requireOpen(PERIOD)).thenThrow(new BusinessRuleException("closed"));

        assertThatThrownBy(() -> gradeService.registerGrade(1L, TEACHER, 5.0))
                .isInstanceOf(BusinessRuleException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void whenEnrollmentMissing_thenNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.registerGrade(99L, TEACHER, 5.0))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void whenGetSectionStudents_thenReturnsStudentsOfItsEnrollments() {
        // Given: two students enrolled in section 11
        when(enrollmentRepository.findBySectionId(11L)).thenReturn(List.of(
                enrollment(1L, 5L, 1L, 11L, PERIOD, null, null), enrollment(2L, 6L, 1L, 11L, PERIOD, null, null)));
        when(studentService.getByIds(List.of(5L, 6L))).thenReturn(List.of(
                student(5L, "11111111-1", "ana@ipt.cl", REGULAR), student(6L, "22222222-2", "bob@ipt.cl", REGULAR)));

        // When / Then
        assertThat(gradeService.getSectionStudents(11L)).hasSize(2);
        verify(sectionService).getById(11L);
    }
}
