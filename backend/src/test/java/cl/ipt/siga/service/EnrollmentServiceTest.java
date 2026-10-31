package cl.ipt.siga.service;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.Day.MONDAY;
import static cl.ipt.siga.entity.enums.StudentStatus.POSTPONED;
import static cl.ipt.siga.entity.enums.StudentStatus.REGULAR;
import static cl.ipt.siga.entity.enums.SubjectResult.APPROVED;
import static cl.ipt.siga.service.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentService studentService;
    @Mock private AcademicPeriodService academicPeriodService;
    @Mock private PeriodRegistrationService periodRegistrationService;
    @Mock private SubjectService subjectService;
    @Mock private PrerequisiteService prerequisiteService;
    @Mock private SectionService sectionService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    // Scenario: student 1 (plan 10) in period 100.
    //   S1, S2, S3: semester 1, no prerequisites, sections 11, 12, 13 (blocks L1, L2, L3)
    //   S4:         semester 2, requires S1, section 14 (block L4)
    private static final Long STUDENT = 1L;
    private static final Long PLAN = 10L;
    private static final Long PERIOD = 100L;
    private static final Long CLOSED_PERIOD = 50L;

    private final SubjectEntity s1 = subject(1L, PLAN, "TAP101", 1);
    private final SubjectEntity s2 = subject(2L, PLAN, "TAP102", 1);
    private final SubjectEntity s3 = subject(3L, PLAN, "TAP103", 1);
    private final SubjectEntity s4 = subject(4L, PLAN, "TAP201", 2);

    // ---------------------------------------------------------------- queries

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenGetByStudentWithoutPeriod_thenReturnsAllEnrollments() {
        when(enrollmentRepository.findByStudentId(STUDENT)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null), enrollment(2L, STUDENT, 2L, 12L, CLOSED_PERIOD, 5.0, APPROVED)));

        assertThat(enrollmentService.getByStudent(STUDENT, null)).hasSize(2);
        verify(studentService).getById(STUDENT);
    }

    @Test
    void whenGetByStudentAndPeriod_thenFiltersByPeriod() {
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(STUDENT, PERIOD))
                .thenReturn(List.of(enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null)));

        assertThat(enrollmentService.getByStudent(STUDENT, PERIOD)).hasSize(1);
        verify(academicPeriodService).getById(PERIOD);
    }

    @Test
    void whenGetAcademicHistory_thenOnlyClosedPeriodsAreIncluded() {
        // Given
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(PERIOD));
        when(enrollmentRepository.findByStudentId(STUDENT)).thenReturn(List.of(
                enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null),
                enrollment(2L, STUDENT, 2L, 12L, CLOSED_PERIOD, 5.0, APPROVED)));

        // When
        List<EnrollmentEntity> history = enrollmentService.getAcademicHistory(STUDENT);

        // Then
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAcademicPeriodId()).isEqualTo(CLOSED_PERIOD);
    }

    @Test
    void whenGetAvailableSubjects_thenExcludesBlockedAndAlreadyEnrolled() {
        // Given: S2 already enrolled now, S4 blocked (S1 not approved)
        givenScenario();
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(STUDENT, PERIOD))
                .thenReturn(List.of(enrollment(1L, STUDENT, 2L, 12L, PERIOD, null, null)));

        // When
        List<SubjectEntity> available = enrollmentService.getAvailableSubjects(STUDENT, PERIOD);

        // Then
        assertThat(available).extracting(SubjectEntity::getCode).containsExactly("TAP101", "TAP103");
    }

    // ---------------------------------------------------------------- enroll: success

    @Test
    void whenEnrollThreeAvailableSubjects_thenEnrollmentsCreatedWithSubjectCopy() {
        // Given
        givenScenario();
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<EnrollmentEntity> created = enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L));

        // Then
        assertThat(created).hasSize(3);
        EnrollmentEntity first = created.get(0);
        assertThat(first.getSubjectId()).isEqualTo(1L);
        assertThat(first.getSectionId()).isEqualTo(11L);
        assertThat(first.getSubjectCode()).isEqualTo("TAP101");      // copy kept for the academic history
        assertThat(first.getSubjectSct()).isEqualTo(5);
        assertThat(first.getEnrollmentDate()).isEqualTo(LocalDate.now());
        assertThat(first.getFinalGrade()).isNull();
        assertThat(first.getResult()).isNull();
    }

    @Test
    void whenAddingToExistingEnrollments_thenTotalIsCounted() {
        // Given: S1 is already enrolled; adding S2 and S3 makes 3 in total
        givenScenario();
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(STUDENT, PERIOD))
                .thenReturn(List.of(enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null)));
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<EnrollmentEntity> created = enrollmentService.enroll(STUDENT, PERIOD, List.of(2L, 3L));

        // Then
        assertThat(created).hasSize(2);
    }

    @Test
    void whenFewerThanThreeSubjectsAvailable_thenMinimumIsWhatIsAvailable() {
        // Given: only S1 and S2 are offered this period
        givenScenario();
        lenient().when(sectionService.findBySubjectAndPeriod(3L, PERIOD)).thenReturn(Optional.empty());
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When / Then
        assertThat(enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L))).hasSize(2);
    }

    // ---------------------------------------------------------------- enroll: student and period

    @Test
    void whenStudentIsNotRegular_thenRejected() {
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", POSTPONED));

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REGULAR");
    }

    @Test
    void whenPeriodIsClosed_thenRejected() {
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(academicPeriodService.requireOpen(PERIOD)).thenThrow(new BusinessRuleException("closed"));

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void whenStudentIsNotRegisteredInPeriod_thenRejected() {
        when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        when(periodRegistrationService.isRegistered(STUDENT, PERIOD)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not registered");
    }

    @Test
    void whenNoSubjectsSelected_thenRejected() {
        givenScenario();

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("At least one");
    }

    @Test
    void whenSameSubjectSelectedTwice_thenRejected() {
        givenScenario();

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 1L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("more than once");
    }

    // ---------------------------------------------------------------- enroll: subject rules

    @Test
    void whenSubjectNotInStudentPlan_thenRejected() {
        givenScenario();

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 999L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("study plan");
    }

    @Test
    void whenSubjectAlreadyEnrolledInPeriod_thenConflict() {
        givenScenario();
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(STUDENT, PERIOD))
                .thenReturn(List.of(enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null)));

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void whenSubjectAlreadyApproved_thenRejected() {
        givenScenario();
        givenApprovedInClosedPeriod(1L);

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already approved");
    }

    @Test
    void whenApprovalIsInAnOpenPeriod_thenItDoesNotCountYet() {
        // Given: S1 has an APPROVED result, but in a period that is still OPEN (grades can still change)
        givenScenario();
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(PERIOD, 200L));
        when(enrollmentRepository.findByStudentIdAndResult(STUDENT, APPROVED))
                .thenReturn(List.of(enrollment(9L, STUDENT, 1L, 11L, 200L, 6.0, APPROVED)));

        // When / Then: S4 still requires S1
        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(4L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requires TAP101");
    }

    @Test
    void whenPrerequisiteNotApproved_thenRejected() {
        givenScenario();

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(4L, 1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requires TAP101");
    }

    @Test
    void whenSubjectNotOfferedInPeriod_thenRejected() {
        givenScenario();
        lenient().when(sectionService.findBySubjectAndPeriod(3L, PERIOD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(3L, 1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not offered");
    }

    // ---------------------------------------------------------------- enroll: period totals

    @Test
    void whenLessThanThreeSubjects_thenRejected() {
        givenScenario();

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("At least 3");
    }

    @Test
    void whenMoreThanSixSubjectsInTotal_thenRejected() {
        // Given: 5 subjects of other semesters already enrolled + 2 new ones = 7
        givenScenario();
        List<EnrollmentEntity> current = new ArrayList<>();
        for (long id = 20; id < 25; id++) {
            current.add(enrollment(id, STUDENT, id, id, PERIOD, null, null));
        }
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(STUDENT, PERIOD)).thenReturn(current);

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("At most 6");
    }

    @Test
    void whenHigherSemesterChosenWhileLowerOneIsAvailable_thenRejected() {
        // Given: S1 approved, so S4 (semester 2) is available; S5 is another semester-2 subject.
        // Choosing S4, S5 and S2 leaves S3 (semester 1, available) out.
        givenScenario();
        givenApprovedInClosedPeriod(1L);
        SubjectEntity s5 = subject(5L, PLAN, "TAP202", 2);
        when(subjectService.getByStudyPlan(PLAN)).thenReturn(List.of(s1, s2, s3, s4, s5));
        lenient().when(sectionService.findBySubjectAndPeriod(5L, PERIOD))
                .thenReturn(Optional.of(section(15L, 5L, PERIOD, 7L, 40)));

        // When / Then
        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(4L, 5L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("TAP103");
    }

    // ---------------------------------------------------------------- enroll: sections

    @Test
    void whenSectionIsFull_thenRejected() {
        givenScenario();
        // lenient: the other sections are also counted (with the default answer, 0 seats taken)
        lenient().when(enrollmentRepository.countBySectionId(13L)).thenReturn(40L);   // capacity 40

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no available seats");
        verify(enrollmentRepository, never()).saveAll(any());
    }

    @Test
    void whenSectionsHaveScheduleConflict_thenRejected() {
        // Given: S3's section also meets on L1, like S1's
        givenScenario();
        when(sectionService.getSchedule(13L)).thenReturn(List.of(block(13L, MONDAY, 1)));

        assertThatThrownBy(() -> enrollmentService.enroll(STUDENT, PERIOD, List.of(1L, 2L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Schedule conflict at L1");
        verify(enrollmentRepository, never()).saveAll(any());
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void whenCancelUngradedEnrollment_thenDeleted() {
        EnrollmentEntity enrollment = enrollment(1L, STUDENT, 1L, 11L, PERIOD, null, null);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        enrollmentService.cancel(1L);

        verify(academicPeriodService).requireOpen(PERIOD);
        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void whenCancelGradedEnrollment_thenRejected() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment(1L, STUDENT, 1L, 11L, PERIOD, 5.0, APPROVED)));

        assertThatThrownBy(() -> enrollmentService.cancel(1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(enrollmentRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- helpers

    // The full scenario is shared by many tests; each test only uses part of it,
    // so these stubs are lenient (Mockito does not complain if a test leaves some unused).
    private void givenScenario() {
        lenient().when(studentService.getById(STUDENT)).thenReturn(student(STUDENT, "12345678-K", "a@ipt.cl", REGULAR));
        lenient().when(periodRegistrationService.isRegistered(STUDENT, PERIOD)).thenReturn(true);
        lenient().when(subjectService.getByStudyPlan(PLAN)).thenReturn(List.of(s1, s2, s3, s4));

        lenient().when(prerequisiteService.getPrerequisites(4L)).thenReturn(List.of(prerequisite(4L, 1L)));

        for (SubjectEntity s : Arrays.asList(s1, s2, s3, s4)) {
            Long sectionId = 10L + s.getId();
            SectionEntity section = section(sectionId, s.getId(), PERIOD, 7L, 40);
            lenient().when(sectionService.findBySubjectAndPeriod(s.getId(), PERIOD)).thenReturn(Optional.of(section));
            lenient().when(sectionService.getSchedule(sectionId))
                    .thenReturn(List.of(block(sectionId, MONDAY, s.getId().intValue())));   // L1, L2, L3, L4
        }
    }

    private void givenApprovedInClosedPeriod(Long subjectId) {
        when(enrollmentRepository.findByStudentIdAndResult(STUDENT, APPROVED))
                .thenReturn(List.of(enrollment(9L, STUDENT, subjectId, 10L + subjectId, CLOSED_PERIOD, 6.0, APPROVED)));
    }
}
