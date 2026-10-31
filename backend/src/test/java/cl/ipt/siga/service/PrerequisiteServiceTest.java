package cl.ipt.siga.service;

import cl.ipt.siga.entity.PrerequisiteEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.PrerequisiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.service.TestData.prerequisite;
import static cl.ipt.siga.service.TestData.subject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrerequisiteServiceTest {

    @Mock
    private PrerequisiteRepository prerequisiteRepository;

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private PrerequisiteService prerequisiteService;

    // TAP201 (semester 2) requires TAP101 (semester 1), both in plan 1.
    // getById is stubbed for two different ids, so the stubs are lenient
    // (strict mode would otherwise report the second id as a possible mistake).
    private void givenTap201AndTap101() {
        lenient().when(subjectService.getById(7L)).thenReturn(subject(7L, 1L, "TAP201", 2));
        lenient().when(subjectService.getById(1L)).thenReturn(subject(1L, 1L, "TAP101", 1));
    }

    @Test
    void whenGetPrerequisites_thenChecksSubjectAndReturnsThem() {
        when(prerequisiteRepository.findBySubjectId(7L)).thenReturn(List.of(prerequisite(7L, 1L)));

        assertThat(prerequisiteService.getPrerequisites(7L)).hasSize(1);
        verify(subjectService).getById(7L);
    }

    @Test
    void whenAddValidPrerequisite_thenSaved() {
        // Given
        givenTap201AndTap101();
        when(prerequisiteRepository.countBySubjectId(7L)).thenReturn(0L);
        when(prerequisiteRepository.existsBySubjectIdAndPrerequisiteId(7L, 1L)).thenReturn(false);
        when(prerequisiteRepository.save(any(PrerequisiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PrerequisiteEntity created = prerequisiteService.addPrerequisite(7L, 1L);

        // Then
        assertThat(created.getSubjectId()).isEqualTo(7L);
        assertThat(created.getPrerequisiteId()).isEqualTo(1L);
    }

    @Test
    void whenAddPrerequisiteFromAnotherPlan_thenRejected() {
        lenient().when(subjectService.getById(7L)).thenReturn(subject(7L, 1L, "TAP201", 2));
        lenient().when(subjectService.getById(50L)).thenReturn(subject(50L, 2L, "TAD101", 1));

        assertThatThrownBy(() -> prerequisiteService.addPrerequisite(7L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("same study plan");
    }

    @Test
    void whenAddPrerequisiteFromSameOrLaterSemester_thenRejected() {
        // TAP101 (semester 1) cannot require TAP201 (semester 2): this also prevents cycles
        givenTap201AndTap101();

        assertThatThrownBy(() -> prerequisiteService.addPrerequisite(1L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("earlier semester");
    }

    @Test
    void whenAddFourthPrerequisite_thenRejected() {
        givenTap201AndTap101();
        when(prerequisiteRepository.countBySubjectId(7L)).thenReturn(3L);

        assertThatThrownBy(() -> prerequisiteService.addPrerequisite(7L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("more than 3");
        verify(prerequisiteRepository, never()).save(any());
    }

    @Test
    void whenAddDuplicatePrerequisite_thenConflict() {
        givenTap201AndTap101();
        when(prerequisiteRepository.existsBySubjectIdAndPrerequisiteId(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> prerequisiteService.addPrerequisite(7L, 1L))
                .isInstanceOf(DuplicateResourceException.class);
        verify(prerequisiteRepository, never()).save(any());
    }

    @Test
    void whenRemoveExistingPrerequisite_thenDeleted() {
        PrerequisiteEntity existing = prerequisite(7L, 1L);
        when(prerequisiteRepository.findBySubjectIdAndPrerequisiteId(7L, 1L)).thenReturn(Optional.of(existing));

        prerequisiteService.removePrerequisite(7L, 1L);

        verify(prerequisiteRepository).delete(existing);
    }

    @Test
    void whenRemoveMissingPrerequisite_thenNotFound() {
        when(prerequisiteRepository.findBySubjectIdAndPrerequisiteId(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prerequisiteService.removePrerequisite(7L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
