package cl.ipt.siga.service;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.entity.ScheduleBlockEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.ScheduleBlockRepository;
import cl.ipt.siga.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.Day.*;
import static cl.ipt.siga.entity.enums.TeacherStatus.ACTIVE;
import static cl.ipt.siga.entity.enums.TeacherStatus.INACTIVE;
import static cl.ipt.siga.service.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private SubjectService subjectService;

    @Mock
    private AcademicPeriodService academicPeriodService;

    @Mock
    private TeacherService teacherService;

    @InjectMocks
    private SectionService sectionService;

    private static final Long SUBJECT = 2L;    // TAP102: TEL 2-2-0 = 4 hours -> 2 blocks
    private static final Long PERIOD = 100L;
    private static final Long TEACHER = 7L;

    private SectionRequest request(int capacity, List<String> schedule) {
        return new SectionRequest(SUBJECT, PERIOD, TEACHER, capacity, schedule);
    }

    // ---------------------------------------------------------------- queries

    @Test
    void whenGetAll_thenReturnsAllSections() {
        when(sectionRepository.findAll()).thenReturn(List.of(section(1L, SUBJECT, PERIOD, TEACHER, 40)));

        assertThat(sectionService.getAll()).hasSize(1);
    }

    @Test
    void whenGetByAcademicPeriod_thenChecksPeriodAndReturnsOffer() {
        when(sectionRepository.findByAcademicPeriodId(PERIOD)).thenReturn(List.of(section(1L, SUBJECT, PERIOD, TEACHER, 40)));

        assertThat(sectionService.getByAcademicPeriod(PERIOD)).hasSize(1);
        verify(academicPeriodService).getById(PERIOD);
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenGetSchedule_thenReturnsSectionBlocks() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section(1L, SUBJECT, PERIOD, TEACHER, 40)));
        when(scheduleBlockRepository.findBySectionId(1L)).thenReturn(List.of(block(1L, MONDAY, 2), block(1L, TUESDAY, 2)));

        assertThat(sectionService.getSchedule(1L)).hasSize(2);
    }

    @Test
    void whenFindBySubjectAndPeriod_thenDelegatesToRepository() {
        when(sectionRepository.findBySubjectIdAndAcademicPeriodId(SUBJECT, PERIOD))
                .thenReturn(Optional.of(section(1L, SUBJECT, PERIOD, TEACHER, 40)));

        assertThat(sectionService.findBySubjectAndPeriod(SUBJECT, PERIOD)).isPresent();
    }

    @Test
    void whenGetByTeacherWithoutPeriod_thenReturnsAllTeacherSections() {
        when(sectionRepository.findByTeacherId(TEACHER)).thenReturn(List.of(section(1L, SUBJECT, PERIOD, TEACHER, 40)));

        assertThat(sectionService.getByTeacher(TEACHER, null)).hasSize(1);
        verify(teacherService).getById(TEACHER);
    }

    @Test
    void whenGetByTeacherAndPeriod_thenFiltersByPeriod() {
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(TEACHER, PERIOD)).thenReturn(List.of());

        assertThat(sectionService.getByTeacher(TEACHER, PERIOD)).isEmpty();
        verify(academicPeriodService).getById(PERIOD);
    }

    // ---------------------------------------------------------------- create

    @Test
    void whenCreateValidSection_thenSectionAndBlocksAreSaved() {
        // Given
        givenSubjectAndActiveTeacher();
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(invocation -> {
            SectionEntity saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        // When: lowercase and spaces are accepted ("m2" -> TUESDAY, module 2)
        SectionEntity created = sectionService.create(request(40, List.of("L2", " m2 ")));

        // Then
        assertThat(created.getId()).isEqualTo(50L);
        assertThat(created.getCapacity()).isEqualTo(40);
        ArgumentCaptor<List<ScheduleBlockEntity>> blocks = blocksCaptor();
        verify(scheduleBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue()).hasSize(2);
        assertThat(blocks.getValue()).allMatch(b -> b.getSectionId().equals(50L));
        assertThat(blocks.getValue().get(1).getDay()).isEqualTo(TUESDAY);
        verify(academicPeriodService).requireOpen(PERIOD);
    }

    @Test
    void whenTeacherHasOtherSectionsWithoutConflict_thenCreated() {
        // Given: the teacher already teaches on W3 in the same period
        givenSubjectAndActiveTeacher();
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(TEACHER, PERIOD))
                .thenReturn(List.of(section(30L, 9L, PERIOD, TEACHER, 40)));
        when(scheduleBlockRepository.findBySectionIdIn(List.of(30L))).thenReturn(List.of(block(30L, WEDNESDAY, 3)));
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        sectionService.create(request(40, List.of("L2", "M2")));

        // Then
        verify(scheduleBlockRepository).saveAll(any());
    }

    @Test
    void whenTeacherAlreadyTeachesAtThatTime_thenConflict() {
        // Given: the teacher already teaches on L2 in the same period
        givenSubjectAndActiveTeacher();
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(TEACHER, PERIOD))
                .thenReturn(List.of(section(30L, 9L, PERIOD, TEACHER, 40)));
        when(scheduleBlockRepository.findBySectionIdIn(List.of(30L))).thenReturn(List.of(block(30L, MONDAY, 2)));

        // When / Then
        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M2"))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("L2");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void whenTeacherIsInactive_thenRejected() {
        when(subjectService.getById(SUBJECT)).thenReturn(tap102());
        when(teacherService.getById(TEACHER)).thenReturn(teacher(TEACHER, "9876543-2", "c@ipt.cl", INACTIVE));

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M2"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void whenPeriodIsClosed_thenRejected() {
        when(subjectService.getById(SUBJECT)).thenReturn(tap102());
        when(academicPeriodService.requireOpen(PERIOD)).thenThrow(new BusinessRuleException("closed"));

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M2"))))
                .isInstanceOf(BusinessRuleException.class);
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void whenCapacityAbove60_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(61, List.of("L2", "M2"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Capacity");
    }

    @Test
    void whenSubjectAlreadyHasSectionInPeriod_thenConflict() {
        givenSubjectAndActiveTeacher();
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(SUBJECT, PERIOD)).thenReturn(true);

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M2"))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void whenScheduleIsEmpty_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void whenBlockHasInvalidFormat_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "Monday"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid schedule block");
    }

    @Test
    void whenBlockHasInvalidDay_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "X2"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid day");
    }

    @Test
    void whenBlockHasInvalidModule_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M7"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("module");
    }

    @Test
    void whenBlockIsRepeated_thenRejected() {
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "l2"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("repeated");
    }

    @Test
    void whenNumberOfBlocksDoesNotMatchTel_thenRejected() {
        // TAP102 has TEL 4, so it needs exactly 2 blocks
        givenSubjectAndActiveTeacher();

        assertThatThrownBy(() -> sectionService.create(request(40, List.of("L2", "M2", "J2"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("needs exactly 2");
        verify(sectionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- helpers

    private cl.ipt.siga.entity.SubjectEntity tap102() {
        return subject(SUBJECT, 1L, "TAP102", 1, 2, 2, 0, 5);
    }

    private void givenSubjectAndActiveTeacher() {
        when(subjectService.getById(SUBJECT)).thenReturn(tap102());
        when(teacherService.getById(TEACHER)).thenReturn(teacher(TEACHER, "9876543-2", "c@ipt.cl", ACTIVE));
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<ScheduleBlockEntity>> blocksCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
