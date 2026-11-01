package cl.ipt.siga.integration;

import cl.ipt.siga.entity.*;
import cl.ipt.siga.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Process 2: academic enrollment of a student in a period
class EnrollmentIntegrationTest extends IntegrationTestBase {

    private CareerEntity career;
    private StudyPlanEntity plan;
    private SubjectEntity tap101, tap102, tap103, tap201;   // TAP201 (semester 2) requires TAP101
    private TeacherEntity carlos, maria;
    private AcademicPeriodEntity period2027a;
    private StudentEntity ana, bob;

    @BeforeEach
    void setUp() {
        career = career("10450");
        plan = currentPlan(career, "2021.3");
        tap101 = subject(plan, "TAP101", 1, 2, 0, 2);
        tap102 = subject(plan, "TAP102", 1, 2, 0, 2);
        tap103 = subject(plan, "TAP103", 1, 2, 0, 2);
        tap201 = subject(plan, "TAP201", 2, 2, 0, 2);
        prerequisiteService.addPrerequisite(tap201.getId(), tap101.getId());

        carlos = teacher("9876543-2", "carlos@ipt.cl");
        maria = teacher("8765432-1", "maria@ipt.cl");
        period2027a = period(2027, 1);
        section(tap101, period2027a, carlos, 40, "L1", "M1");
        section(tap102, period2027a, carlos, 40, "L2", "M2");
        section(tap103, period2027a, carlos, 1, "L3", "M3");    // only one seat
        section(tap201, period2027a, carlos, 40, "L4", "M4");

        ana = student(career, "11111111-1", "ana@ipt.cl");
        bob = student(career, "22222222-2", "bob@ipt.cl");
        register(ana, period2027a);
        register(bob, period2027a);
    }

    // ---------------------------------------------------------------- successful scenarios

    @Test
    void whenEnrollingAvailableSubjects_thenEnrollmentsKeepACopyOfTheSubject() {
        // When
        List<EnrollmentEntity> created = enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(tap101, tap102, tap103));

        // Then
        assertThat(created).hasSize(3);
        assertThat(enrollmentRepository.findByStudentIdAndAcademicPeriodId(ana.getId(), period2027a.getId())).hasSize(3);

        // And: renaming the subject later does not change what was recorded for the student
        SubjectEntity renamed = subjectService.getById(tap101.getId());
        SubjectEntity data = new SubjectEntity();
        data.setCode(renamed.getCode());
        data.setName("Programming Fundamentals (new name)");
        data.setSemester(renamed.getSemester());
        data.setTheoryHours(renamed.getTheoryHours());
        data.setExerciseHours(renamed.getExerciseHours());
        data.setLabHours(renamed.getLabHours());
        data.setSct(renamed.getSct());
        subjectService.update(tap101.getId(), data);

        EnrollmentEntity stored = enrollmentRepository.findById(enrollmentOf(created, tap101).getId()).orElseThrow();
        assertThat(stored.getSubjectName()).isEqualTo("Subject TAP101");
    }

    @Test
    void whenAnEnrollmentIsCancelled_thenItsSeatCanBeTakenByAnotherStudent() {
        // Given: Ana takes the only seat of TAP103, so Bob cannot enroll it
        List<EnrollmentEntity> anaEnrollments = enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(tap101, tap102, tap103));
        assertThatThrownBy(() -> enrollmentService.enroll(bob.getId(), period2027a.getId(), ids(tap101, tap102, tap103)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no available seats");

        // When: Ana cancels TAP103
        enrollmentService.cancel(enrollmentOf(anaEnrollments, tap103).getId());

        // Then: Bob gets the seat
        assertThat(enrollmentService.enroll(bob.getId(), period2027a.getId(), ids(tap101, tap102, tap103))).hasSize(3);
    }

    @Test
    void whenSubjectWasFailedInAClosedPeriod_thenItCanBeEnrolledAgain() {
        // Given: Ana fails TAP101 in 2027-1 and the period is closed
        List<EnrollmentEntity> first = enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(tap101, tap102, tap103));
        grade(enrollmentOf(first, tap101), carlos, 3.0);
        grade(enrollmentOf(first, tap102), carlos, 5.0);
        grade(enrollmentOf(first, tap103), carlos, 6.0);
        periodClosingService.close(period2027a.getId());

        // And: in 2027-2 only TAP101 is available to her (TAP201 still requires TAP101)
        AcademicPeriodEntity period2027b = period(2027, 2);
        section(tap101, period2027b, carlos, 40, "L1", "M1");
        register(ana, period2027b);

        // When
        List<EnrollmentEntity> second = enrollmentService.enroll(ana.getId(), period2027b.getId(), ids(tap101));

        // Then: one subject is enough because it is the only one available
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getSubjectId()).isEqualTo(tap101.getId());
    }

    // ---------------------------------------------------------------- rejection scenarios

    @Test
    void whenStudentIsNotRegisteredInThePeriod_thenRejected() {
        StudentEntity carla = student(career, "33333333-3", "carla@ipt.cl");

        assertThatThrownBy(() -> enrollmentService.enroll(carla.getId(), period2027a.getId(), ids(tap101, tap102, tap103)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not registered");
        assertThat(enrollmentRepository.count()).isZero();
    }

    @Test
    void whenPrerequisiteIsNotApproved_thenRejected() {
        assertThatThrownBy(() -> enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(tap201, tap101, tap102)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requires TAP101");
        assertThat(enrollmentRepository.count()).isZero();
    }

    @Test
    void whenTwoSectionsOverlap_thenNoSubjectIsEnrolled() {
        // Given: TAP104 meets on L1, like TAP101 (taught by another teacher, so the section itself is valid)
        SubjectEntity tap104 = subject(plan, "TAP104", 1, 2, 0, 2);
        section(tap104, period2027a, maria, 40, "L1", "W5");

        // When / Then: the whole request is rejected, not only TAP104
        assertThatThrownBy(() -> enrollmentService.enroll(ana.getId(), period2027a.getId(), ids(tap101, tap102, tap104)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Schedule conflict at L1");
        assertThat(enrollmentRepository.count()).isZero();
    }
}
