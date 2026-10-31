package cl.ipt.siga.service;

import cl.ipt.siga.entity.CareerEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.CareerStatus.ACTIVE;
import static cl.ipt.siga.entity.enums.CareerStatus.INACTIVE;
import static cl.ipt.siga.service.TestData.career;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;   // fake: its answers are programmed in each test

    @InjectMocks
    private CareerService careerService;         // real: receives the fake through its constructor

    @Test
    void whenGetAll_thenReturnsAllCareers() {
        // Given
        when(careerRepository.findAll()).thenReturn(List.of(career(1L, "10450", ACTIVE), career(2L, "10451", ACTIVE)));

        // When
        List<CareerEntity> careers = careerService.getAll();

        // Then
        assertThat(careers).hasSize(2);
    }

    @Test
    void whenGetByIdExisting_thenReturnsCareer() {
        // Given
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450", ACTIVE)));

        // When
        CareerEntity career = careerService.getById(1L);

        // Then
        assertThat(career.getCode()).isEqualTo("10450");
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        // Given
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> careerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenCreateWithNewCode_thenSavesIgnoringClientId() {
        // Given
        CareerEntity input = career(5L, "10450", ACTIVE);   // the id sent by the client must be ignored
        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(CareerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CareerEntity created = careerService.create(input);

        // Then
        assertThat(created.getId()).isNull();
        verify(careerRepository).save(input);
    }

    @Test
    void whenCreateWithDuplicateCode_thenConflictAndNothingSaved() {
        // Given
        when(careerRepository.existsByCode("10450")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> careerService.create(career(null, "10450", ACTIVE)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("10450");
        verify(careerRepository, never()).save(any());
    }

    @Test
    void whenUpdateWithSameCode_thenUpdatesNameDescriptionAndStatus() {
        // Given
        CareerEntity existing = career(1L, "10450", ACTIVE);
        CareerEntity data = career(null, "10450", INACTIVE);
        data.setName("New name");
        data.setDescription("New description");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(careerRepository.save(existing)).thenReturn(existing);

        // When
        CareerEntity updated = careerService.update(1L, data);

        // Then
        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getStatus()).isEqualTo(INACTIVE);
    }

    @Test
    void whenUpdateChangingCode_thenRejected() {
        // Given
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450", ACTIVE)));

        // When / Then
        assertThatThrownBy(() -> careerService.update(1L, career(null, "99999", ACTIVE)))
                .isInstanceOf(BusinessRuleException.class);
        verify(careerRepository, never()).save(any());
    }
}
