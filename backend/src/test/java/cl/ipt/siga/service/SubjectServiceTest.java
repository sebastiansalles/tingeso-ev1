package cl.ipt.siga.service;

import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.service.TestData.subject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudyPlanService studyPlanService;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void whenGetAll_thenReturnsAllSubjects() {
        when(subjectRepository.findAll()).thenReturn(List.of(subject(1L, 1L, "TAP101", 1)));

        assertThat(subjectService.getAll()).hasSize(1);
    }

    @Test
    void whenGetByStudyPlan_thenChecksPlanAndReturnsItsSubjects() {
        when(subjectRepository.findByStudyPlanId(1L)).thenReturn(List.of(subject(1L, 1L, "TAP101", 1)));

        assertThat(subjectService.getByStudyPlan(1L)).hasSize(1);
        verify(studyPlanService).getById(1L);
    }

    @Test
    void whenGetByIdExisting_thenReturnsSubject() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, 1L, "TAP101", 1)));

        assertThat(subjectService.getById(1L).getCode()).isEqualTo("TAP101");
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenCreateValidSubject_thenSaved() {
        // Given: TAP101 from Appendix A (TEL 2-2-2, 6 SCT)
        SubjectEntity input = subject(7L, 1L, "TAP101", 1, 2, 2, 2, 6);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(false);
        when(subjectRepository.save(input)).thenReturn(input);

        // When
        SubjectEntity created = subjectService.create(input);

        // Then
        assertThat(created.getId()).isNull();
        verify(studyPlanService).getById(1L);
    }

    @Test
    void whenCreateWithSemesterOutOfRange_thenRejected() {
        assertThatThrownBy(() -> subjectService.create(subject(null, 1L, "X", 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Semester");
    }

    @Test
    void whenCreateWithSctOutOfRange_thenRejected() {
        assertThatThrownBy(() -> subjectService.create(subject(null, 1L, "X", 1, 2, 0, 2, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SCT");
    }

    @Test
    void whenCreateWithNegativeHours_thenRejected() {
        assertThatThrownBy(() -> subjectService.create(subject(null, 1L, "X", 1, 2, -1, 2, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("less than 0");
    }

    @Test
    void whenCreateWithEightWeeklyHours_thenRejected() {
        assertThatThrownBy(() -> subjectService.create(subject(null, 1L, "X", 1, 4, 2, 2, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("less than 8");
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void whenCreateDuplicateCodeInPlan_thenConflict() {
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.create(subject(null, 1L, "TAP101", 1)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void whenUpdateKeepingCode_thenUpdatesWithoutCheckingDuplicates() {
        // Given
        SubjectEntity existing = subject(1L, 1L, "TAP101", 1);
        SubjectEntity data = subject(null, 2L, "TAP101", 2, 2, 2, 2, 6);   // studyPlanId 2 must be ignored
        data.setName("New name");
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(subjectRepository.save(existing)).thenReturn(existing);

        // When
        SubjectEntity updated = subjectService.update(1L, data);

        // Then
        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getSemester()).isEqualTo(2);
        assertThat(updated.getSct()).isEqualTo(6);
        assertThat(updated.getStudyPlanId()).isEqualTo(1L);   // a subject never moves to another plan
        verify(subjectRepository, never()).existsByStudyPlanIdAndCode(anyLong(), anyString());
    }

    @Test
    void whenUpdateToFreeCode_thenUpdated() {
        SubjectEntity existing = subject(1L, 1L, "TAP101", 1);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP199")).thenReturn(false);
        when(subjectRepository.save(existing)).thenReturn(existing);

        assertThat(subjectService.update(1L, subject(null, 1L, "TAP199", 1)).getCode()).isEqualTo("TAP199");
    }

    @Test
    void whenUpdateToCodeUsedInPlan_thenConflict() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, 1L, "TAP101", 1)));
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP102")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.update(1L, subject(null, 1L, "TAP102", 1)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void whenUpdateWithInvalidData_thenRejected() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, 1L, "TAP101", 1)));

        assertThatThrownBy(() -> subjectService.update(1L, subject(null, 1L, "TAP101", 0)))
                .isInstanceOf(BusinessRuleException.class);
        verify(subjectRepository, never()).save(any());
    }
}
