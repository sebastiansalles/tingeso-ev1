package cl.ipt.siga.service;

import cl.ipt.siga.entity.StudyPlanEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.StudyPlanStatus.CURRENT;
import static cl.ipt.siga.entity.enums.StudyPlanStatus.NOT_CURRENT;
import static cl.ipt.siga.service.TestData.studyPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private CareerService careerService;

    @InjectMocks
    private StudyPlanService studyPlanService;

    @Test
    void whenGetAll_thenReturnsAllPlans() {
        when(studyPlanRepository.findAll()).thenReturn(List.of(studyPlan(1L, 1L, "2021.3", CURRENT)));

        assertThat(studyPlanService.getAll()).hasSize(1);
    }

    @Test
    void whenGetByCareer_thenChecksCareerAndReturnsItsPlans() {
        // Given
        when(studyPlanRepository.findByCareerId(1L)).thenReturn(List.of(
                studyPlan(1L, 1L, "2021.3", NOT_CURRENT), studyPlan(2L, 1L, "2024.1", CURRENT)));

        // When
        List<StudyPlanEntity> plans = studyPlanService.getByCareer(1L);

        // Then
        assertThat(plans).hasSize(2);
        verify(careerService).getById(1L);
    }

    @Test
    void whenGetByCareerMissing_thenNotFound() {
        when(careerService.getById(99L)).thenThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> studyPlanService.getByCareer(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(studyPlanRepository, never()).findByCareerId(any());
    }

    @Test
    void whenGetByIdExisting_thenReturnsPlan() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(studyPlan(1L, 1L, "2021.3", CURRENT)));

        assertThat(studyPlanService.getById(1L).getIdentifier()).isEqualTo("2021.3");
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(studyPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenCreateCurrentPlan_thenPreviousCurrentBecomesNotCurrent() {
        // Given
        StudyPlanEntity previous = studyPlan(1L, 1L, "2021.3", CURRENT);
        StudyPlanEntity input = studyPlan(9L, 1L, "2024.1", CURRENT);
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2024.1")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, CURRENT)).thenReturn(Optional.of(previous));
        when(studyPlanRepository.save(any(StudyPlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        StudyPlanEntity created = studyPlanService.create(input);

        // Then
        assertThat(previous.getStatus()).isEqualTo(NOT_CURRENT);
        assertThat(created.getId()).isNull();
        assertThat(created.getStatus()).isEqualTo(CURRENT);
        verify(studyPlanRepository).save(previous);
        verify(studyPlanRepository).save(input);
    }

    @Test
    void whenCreateFirstCurrentPlan_thenOnlyTheNewPlanIsSaved() {
        // Given
        StudyPlanEntity input = studyPlan(null, 1L, "2021.3", CURRENT);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, CURRENT)).thenReturn(Optional.empty());
        when(studyPlanRepository.save(input)).thenReturn(input);

        // When
        studyPlanService.create(input);

        // Then
        verify(studyPlanRepository, times(1)).save(any());
    }

    @Test
    void whenCreateNotCurrentPlan_thenCurrentPlanIsNotTouched() {
        // Given
        StudyPlanEntity input = studyPlan(null, 1L, "2019.1", NOT_CURRENT);
        when(studyPlanRepository.save(input)).thenReturn(input);

        // When
        studyPlanService.create(input);

        // Then
        verify(studyPlanRepository, never()).findByCareerIdAndStatus(any(), any());
    }

    @Test
    void whenCreateDuplicateIdentifier_thenConflict() {
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2021.3")).thenReturn(true);

        assertThatThrownBy(() -> studyPlanService.create(studyPlan(null, 1L, "2021.3", CURRENT)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void whenCreateForMissingCareer_thenNotFound() {
        when(careerService.getById(99L)).thenThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> studyPlanService.create(studyPlan(null, 99L, "2021.3", CURRENT)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void whenGetCurrentByCareer_thenReturnsCurrentPlan() {
        when(studyPlanRepository.findByCareerIdAndStatus(1L, CURRENT))
                .thenReturn(Optional.of(studyPlan(2L, 1L, "2024.1", CURRENT)));

        assertThat(studyPlanService.getCurrentByCareer(1L).getId()).isEqualTo(2L);
    }

    @Test
    void whenGetCurrentByCareerWithoutCurrentPlan_thenRejected() {
        when(studyPlanRepository.findByCareerIdAndStatus(1L, CURRENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.getCurrentByCareer(1L))
                .isInstanceOf(BusinessRuleException.class);
    }
}
