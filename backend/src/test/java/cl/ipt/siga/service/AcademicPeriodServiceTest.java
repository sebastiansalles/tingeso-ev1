package cl.ipt.siga.service;


import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.entity.enums.PeriodStatus;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.service.TestData.period;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository; //fake repo

    @InjectMocks
    private AcademicPeriodService academicPeriodService; //real service to test

    //getAll test

    @Test
    void whenGetAll_thenReturnsAllPeriods(){
        //given
        when(academicPeriodRepository.findAll()).thenReturn(List.of(
                period(1L, 2025, 1, PeriodStatus.CLOSED),
                period(2L, 2026, 1, PeriodStatus.OPEN)
        ));
        //When
        List<AcademicPeriodEntity> result = academicPeriodService.getAll();

        //Then
        assertThat(result).hasSize(2);
    }

    //test for getById()
    @Test
    void whenGetByIdExisting_thenReturnsPeriod(){
        //given
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period(1L, 2026, 1, PeriodStatus.OPEN)));

        //when
        AcademicPeriodEntity result = academicPeriodService.getById(1L);

        //then
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getSemester()).isEqualTo(1);
    }
    @Test
    void whenGetByIdMissing_thenNotFound() {
        // Given
        when(academicPeriodRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> academicPeriodService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // create test

    @Test
    void whenCreateValidPeriod_thenSetsOpenIgnoresIdAndSaves() {
        // Given
        AcademicPeriodEntity input = period(10L, 2026, 1, PeriodStatus.CLOSED);
        when(academicPeriodRepository.existsByYearAndSemester(2026, 1)).thenReturn(false);
        when(academicPeriodRepository.save(any(AcademicPeriodEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AcademicPeriodEntity created = academicPeriodService.create(input);

        // Then
        assertThat(created.getId()).isNull();
        assertThat(created.getStatus()).isEqualTo(PeriodStatus.OPEN);
        verify(academicPeriodRepository).save(input);
    }

    @Test
    void whenCreateWithInvalidSemester_thenRejected() {
        // Given
        AcademicPeriodEntity input = period(null, 2026, 3, PeriodStatus.OPEN);

        // When / Then
        assertThatThrownBy(() -> academicPeriodService.create(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Semester must be 1 or 2");
        verify(academicPeriodRepository, never()).save(any());
    }

    @Test
    void whenCreateWithInvalidYear_thenRejected() {
        // Given
        AcademicPeriodEntity tooOld = period(null, 1999, 1, PeriodStatus.OPEN);
        AcademicPeriodEntity tooFuture = period(null, 2101, 2, PeriodStatus.OPEN);

        // When / Then
        assertThatThrownBy(() -> academicPeriodService.create(tooOld))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> academicPeriodService.create(tooFuture))
                .isInstanceOf(BusinessRuleException.class);
        verify(academicPeriodRepository, never()).save(any());
    }

    @Test
    void whenCreateDuplicatePeriod_thenConflictAndNothingSaved() {
        // Given
        AcademicPeriodEntity input = period(null, 2026, 1, PeriodStatus.OPEN);
        when(academicPeriodRepository.existsByYearAndSemester(2026, 1)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> academicPeriodService.create(input))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("2026-1");
        verify(academicPeriodRepository, never()).save(any());
    }

    // requireOpen()

    @Test
    void whenRequireOpenOnOpenPeriod_thenReturnsPeriod() {
        // Given
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(1L, 2026, 1, PeriodStatus.OPEN)));

        // When
        AcademicPeriodEntity result = academicPeriodService.requireOpen(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(PeriodStatus.OPEN);
    }

    @Test
    void whenRequireOpenOnClosedPeriod_thenRejected() {
        // Given
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(1L, 2025, 2, PeriodStatus.CLOSED)));

        // When / Then
        assertThatThrownBy(() -> academicPeriodService.requireOpen(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("is closed");
    }

    // getOpenPeriodIds()

    @Test
    void whenGetOpenPeriodIds_thenReturnsOnlyIds() {
        // Given
        when(academicPeriodRepository.findByStatus(PeriodStatus.OPEN)).thenReturn(List.of(
                period(5L, 2026, 1, PeriodStatus.OPEN),
                period(8L, 2026, 2, PeriodStatus.OPEN)
        ));

        // When
        List<Long> ids = academicPeriodService.getOpenPeriodIds();

        // Then
        assertThat(ids).containsExactly(5L, 8L);
    }

    // markAsClosed()

    @Test
    void whenMarkAsClosed_thenChangesStatusAndSaves() {
        // Given
        AcademicPeriodEntity openPeriod = period(1L, 2026, 1, PeriodStatus.OPEN);
        when(academicPeriodRepository.save(openPeriod)).thenReturn(openPeriod);

        // When
        AcademicPeriodEntity closedPeriod = academicPeriodService.markAsClosed(openPeriod);

        // Then
        assertThat(closedPeriod.getStatus()).isEqualTo(PeriodStatus.CLOSED);
        verify(academicPeriodRepository).save(openPeriod);
    }
}