package cl.ipt.siga.integration;

import cl.ipt.siga.entity.*;
import cl.ipt.siga.entity.enums.PeriodStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Process 3: final grades and closing of the academic period
class PeriodClosingIntegrationTest extends IntegrationTestBase {

    // A small plan of 3 subjects, so a student can complete it in one period
    private SubjectEntity c101, c102, c103;
    private TeacherEntity carlos;
    private AcademicPeriodEntity period2027a;
    private StudentEntity ana;

    @BeforeEach
    void setUp() {
        CareerEntity career = career("20000");
        StudyPlanEntity plan = currentPlan(career, "2026.1");
        c101 = subject(plan, "C101", 1, 2, 0, 2);
        c102 = subject(plan, "C102", 1, 2, 0, 2);
        c103 = subject(plan, "C103", 1, 2, 0, 2);

        carlos = teacher("9876543-2", "carlos@ipt.cl");
        period2027a = period(2027, 1);
        section(c101, period2027a, carlos, 40, "L1", "M1");
        section(c102, period2027a, carlos, 40, "L2", "M2");
        section(c103, period2027a, carlos, 40, "L3", "M3");

        ana = student(career, "11111111-1", "ana@ipt.cl");
        register(ana, period2027a);
    }

    private List<EnrollmentEntity> enrollAll() {
        return enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(c101, c102, c103));
    }

    // ---------------------------------------------------------------- successful scenarios

    @Test
    void whenAllGradesAreRegistered_thenPeriodIsClosedAndResultsBecomeHistory() {
        // Given: one subject failed for the first time
        List<EnrollmentEntity> enrollments = enrollAll();
        grade(enrollmentOf(enrollments, c101), carlos, 5.0);
        grade(enrollmentOf(enrollments, c102), carlos, 3.5);
        grade(enrollmentOf(enrollments, c103), carlos, 6.0);

        // When
        AcademicPeriodEntity closed = periodClosingService.close(period2027a.getId());

        // Then
        assertThat(closed.getStatus()).isEqualTo(PeriodStatus.CLOSED);
        assertThat(enrollmentService.getAcademicHistory(ana.getId())).hasSize(3);
        assertThat(studentService.getById(ana.getId()).getStatus()).isEqualTo(StudentStatus.REGULAR);
    }

    @Test
    void whenSameSubjectIsFailedTwice_thenStudentIsEliminatedKeepingHistory() {
        // Given: C101 failed in 2027-1
        List<EnrollmentEntity> first = enrollAll();
        grade(enrollmentOf(first, c101), carlos, 3.0);
        grade(enrollmentOf(first, c102), carlos, 5.0);
        grade(enrollmentOf(first, c103), carlos, 5.0);
        periodClosingService.close(period2027a.getId());

        // And: C101 failed again in 2027-2
        AcademicPeriodEntity period2027b = period(2027, 2);
        section(c101, period2027b, carlos, 40, "L1", "M1");
        register(ana, period2027b);
        List<EnrollmentEntity> second = enrollmentService.enroll(ana.getId(), period2027b.getId(), ids(c101));
        grade(second.get(0), carlos, 2.0);

        // When
        periodClosingService.close(period2027b.getId());

        // Then
        assertThat(studentService.getById(ana.getId()).getStatus()).isEqualTo(StudentStatus.ELIMINATED);
        assertThat(enrollmentService.getAcademicHistory(ana.getId())).hasSize(4);
    }

    @Test
    void whenWholeStudyPlanIsApproved_thenStudentGraduates() {
        // Given
        List<EnrollmentEntity> enrollments = enrollAll();
        grade(enrollmentOf(enrollments, c101), carlos, 4.0);
        grade(enrollmentOf(enrollments, c102), carlos, 5.5);
        grade(enrollmentOf(enrollments, c103), carlos, 7.0);

        // When
        periodClosingService.close(period2027a.getId());

        // Then
        assertThat(studentService.getById(ana.getId()).getStatus()).isEqualTo(StudentStatus.GRADUATED);
    }

    // ---------------------------------------------------------------- rejection scenarios

    @Test
    void whenGradesAreMissing_thenRejectedAndPeriodStaysOpen() {
        // Given: C103 has no grade
        List<EnrollmentEntity> enrollments = enrollAll();
        grade(enrollmentOf(enrollments, c101), carlos, 5.0);
        grade(enrollmentOf(enrollments, c102), carlos, 5.0);

        // When / Then: the incomplete section is reported
        assertThatThrownBy(() -> periodClosingService.close(period2027a.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("C103");
        assertThat(academicPeriodService.getById(period2027a.getId()).getStatus()).isEqualTo(PeriodStatus.OPEN);
        assertThat(studentService.getById(ana.getId()).getStatus()).isEqualTo(StudentStatus.REGULAR);
    }

    @Test
    void whenPeriodIsAlreadyClosed_thenItCannotBeClosedAgain() {
        // Given
        periodClosingService.close(period2027a.getId());

        // When / Then
        assertThatThrownBy(() -> periodClosingService.close(period2027a.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void whenGradeIsChangedAfterClosing_thenRejectedAndGradeIsKept() {
        // Given
        List<EnrollmentEntity> enrollments = enrollAll();
        for (EnrollmentEntity enrollment : enrollments) {
            grade(enrollment, carlos, 5.0);
        }
        periodClosingService.close(period2027a.getId());
        EnrollmentEntity c101Enrollment = enrollmentOf(enrollments, c101);

        // When / Then
        assertThatThrownBy(() -> grade(c101Enrollment, carlos, 7.0))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(enrollmentRepository.findById(c101Enrollment.getId()).orElseThrow().getFinalGrade()).isEqualTo(5.0);
    }
}
