package cl.ipt.siga.integration;

import cl.ipt.siga.entity.*;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Process 1: creation of the academic offer (sections with schedule and teacher)
class SectionCreationIntegrationTest extends IntegrationTestBase {

    private SubjectEntity tap101;    // TEL 2-2-2 -> 3 blocks
    private SubjectEntity tap102;    // TEL 2-2-0 -> 2 blocks
    private TeacherEntity carlos;
    private AcademicPeriodEntity period2027a;

    @BeforeEach
    void setUp() {
        StudyPlanEntity plan = currentPlan(career("10450"), "2021.3");
        tap101 = subject(plan, "TAP101", 1, 2, 2, 2);
        tap102 = subject(plan, "TAP102", 1, 2, 2, 0);
        carlos = teacher("9876543-2", "carlos@ipt.cl");
        period2027a = period(2027, 1);
    }

    // ---------------------------------------------------------------- successful scenarios

    @Test
    void whenSectionIsValid_thenSectionAndScheduleArePersisted() {
        // When
        SectionEntity section = section(tap101, period2027a, carlos, 40, "L1", "W1", "V1");

        // Then
        assertThat(section.getId()).isNotNull();
        assertThat(scheduleBlockRepository.findBySectionId(section.getId())).hasSize(3);
        assertThat(sectionService.findBySubjectAndPeriod(tap101.getId(), period2027a.getId())).isPresent();
    }

    @Test
    void whenTeacherSchedulesDoNotOverlap_thenTeacherGetsBothSections() {
        // When
        section(tap101, period2027a, carlos, 40, "L1", "W1", "V1");
        section(tap102, period2027a, carlos, 40, "L2", "M2");

        // Then
        assertThat(sectionService.getByTeacher(carlos.getId(), period2027a.getId())).hasSize(2);
    }

    @Test
    void whenSameSubjectIsOfferedInAnotherPeriod_thenBothSectionsExist() {
        // Given
        AcademicPeriodEntity period2027b = period(2027, 2);

        // When: same subject, same teacher and same blocks, but different periods
        section(tap101, period2027a, carlos, 40, "L1", "W1", "V1");
        section(tap101, period2027b, carlos, 40, "L1", "W1", "V1");

        // Then
        assertThat(sectionRepository.count()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- rejection scenarios

    @Test
    void whenTeacherAlreadyTeachesAtThatTime_thenRejectedAndNothingSaved() {
        // Given: Carlos teaches TAP101 on L1
        section(tap101, period2027a, carlos, 40, "L1", "W1", "V1");

        // When / Then: TAP102 also on L1 with Carlos
        assertThatThrownBy(() -> section(tap102, period2027a, carlos, 40, "L1", "M2"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("L1");
        assertThat(sectionRepository.count()).isEqualTo(1);
        assertThat(scheduleBlockRepository.count()).isEqualTo(3);
    }

    @Test
    void whenBlocksDoNotMatchTel_thenRejected() {
        // TAP102 has TEL 4, so it needs exactly 2 blocks
        assertThatThrownBy(() -> section(tap102, period2027a, carlos, 40, "L2", "M2", "J2"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("needs exactly 2");
        assertThat(sectionRepository.count()).isZero();
    }

    @Test
    void whenPeriodIsClosed_thenRejected() {
        // Given: a period without enrollments can be closed right away
        periodClosingService.close(period2027a.getId());

        // When / Then
        assertThatThrownBy(() -> section(tap101, period2027a, carlos, 40, "L1", "W1", "V1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed");
        assertThat(sectionRepository.count()).isZero();
    }
}
