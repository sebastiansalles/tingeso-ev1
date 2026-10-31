package cl.ipt.siga.service;

import cl.ipt.siga.entity.PeriodRegistrationEntity;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.repository.PeriodRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cl.ipt.siga.entity.enums.PeriodStatus.OPEN;
import static cl.ipt.siga.entity.enums.StudentStatus.REGULAR;
import static cl.ipt.siga.service.TestData.period;
import static cl.ipt.siga.service.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodRegistrationServiceTest {

    @Mock private PeriodRegistrationRepository periodRegistrationRepository;
    @Mock private StudentService studentService;
    @Mock private AcademicPeriodService academicPeriodService;

    @InjectMocks
    private PeriodRegistrationService periodRegistrationService;

    @Test
    void whenGetByAcademicPeriod_thenReturnsRegistrations() {
        when(periodRegistrationRepository.findByAcademicPeriodId(1L)).thenReturn(List.of(new PeriodRegistrationEntity()));

        assertThat(periodRegistrationService.getByAcademicPeriod(1L)).hasSize(1);
        verify(academicPeriodService).getById(1L);
    }

    @Test
    void whenRegisterStudent_thenSaved() {
        when(studentService.getById(1L)).thenReturn(student(1L, "12345678-K", "a@ipt.cl", REGULAR));
        when(academicPeriodService.requireOpen(1L)).thenReturn(period(1L, 2027, 1, OPEN));
        when(periodRegistrationRepository.save(any(PeriodRegistrationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PeriodRegistrationEntity created = periodRegistrationService.register(1L, 1L);

        assertThat(created.getStudentId()).isEqualTo(1L);
        assertThat(created.getAcademicPeriodId()).isEqualTo(1L);
    }

    @Test
    void whenRegisterTwice_thenConflict() {
        when(studentService.getById(1L)).thenReturn(student(1L, "12345678-K", "a@ipt.cl", REGULAR));
        when(academicPeriodService.requireOpen(1L)).thenReturn(period(1L, 2027, 1, OPEN));
        when(periodRegistrationRepository.existsByStudentIdAndAcademicPeriodId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> periodRegistrationService.register(1L, 1L))
                .isInstanceOf(DuplicateResourceException.class);
        verify(periodRegistrationRepository, never()).save(any());
    }
}