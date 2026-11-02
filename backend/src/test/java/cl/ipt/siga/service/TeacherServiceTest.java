package cl.ipt.siga.service;

import cl.ipt.siga.entity.TeacherEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.TeacherStatus.ACTIVE;
import static cl.ipt.siga.entity.enums.TeacherStatus.INACTIVE;
import static cl.ipt.siga.service.TestData.teacher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import cl.ipt.siga.security.KeycloakAdminClient;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicPeriodService academicPeriodService;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void whenGetAll_thenReturnsAllTeachers() {
        when(teacherRepository.findAll()).thenReturn(List.of(teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE)));

        assertThat(teacherService.getAll()).hasSize(1);
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- create

    @Test
    void whenCreateValidTeacher_thenNormalizedAndActive() {
        // Given
        TeacherEntity request = teacher(5L, "9.876.543-2", "C.Rojas@IPT.cl", INACTIVE);
        when(teacherRepository.save(any(TeacherEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeacherEntity created = teacherService.create(request);

        // Then
        assertThat(created.getRun()).isEqualTo("9876543-2");
        assertThat(created.getEmail()).isEqualTo("c.rojas@ipt.cl");
        assertThat(created.getStatus()).isEqualTo(ACTIVE);
        assertThat(created.getId()).isNull();
        verify(keycloakAdminClient).createUser("c.rojas@ipt.cl", created.getFullName(), "9876543-2", "TEACHER");
    }

    @Test
    void whenCreateWithoutProfessionalTitle_thenRejected() {
        TeacherEntity request = teacher(null, "9876543-2", "c@ipt.cl", ACTIVE);
        request.setProfessionalTitle(null);

        assertThatThrownBy(() -> teacherService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Professional title");
    }

    @Test
    void whenCreateWithExistingRun_thenConflict() {
        when(teacherRepository.existsByRun("9876543-2")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.create(teacher(null, "9876543-2", "c@ipt.cl", ACTIVE)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void whenCreateWithExistingEmail_thenConflict() {
        when(teacherRepository.existsByEmail("c@ipt.cl")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.create(teacher(null, "9876543-2", "c@ipt.cl", ACTIVE)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(teacherRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    void whenUpdateWithNewData_thenUpdated() {
        // Given
        TeacherEntity existing = teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE);
        TeacherEntity data = teacher(null, "11.111.111-1", "new@ipt.cl", INACTIVE);
        data.setAcademicDegree("Doctor");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.save(existing)).thenReturn(existing);

        // When
        TeacherEntity updated = teacherService.update(1L, data);

        // Then
        assertThat(updated.getRun()).isEqualTo("11111111-1");
        assertThat(updated.getEmail()).isEqualTo("c@ipt.cl");   // the email is never changed
        assertThat(updated.getAcademicDegree()).isEqualTo("Doctor");
        assertThat(updated.getStatus()).isEqualTo(ACTIVE);   // status has its own method
    }

    @Test
    void whenUpdateToRunOfAnotherTeacher_thenConflict() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE)));
        when(teacherRepository.existsByRun("11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.update(1L, teacher(null, "11111111-1", "c@ipt.cl", ACTIVE)))
                .isInstanceOf(DuplicateResourceException.class);
    }



    // ---------------------------------------------------------------- status

    @Test
    void whenSetInactiveWithSectionsInOpenPeriod_thenRejected() {
        // Given
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE)));
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(100L));
        when(sectionRepository.existsByTeacherIdAndAcademicPeriodIdIn(1L, List.of(100L))).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> teacherService.changeStatus(1L, INACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("open period");
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void whenSetInactiveWithoutSectionsInOpenPeriods_thenSaved() {
        TeacherEntity existing = teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of(100L));
        when(sectionRepository.existsByTeacherIdAndAcademicPeriodIdIn(1L, List.of(100L))).thenReturn(false);
        when(teacherRepository.save(existing)).thenReturn(existing);

        assertThat(teacherService.changeStatus(1L, INACTIVE).getStatus()).isEqualTo(INACTIVE);
    }

    @Test
    void whenSetInactiveWithNoOpenPeriods_thenSectionsAreNotChecked() {
        TeacherEntity existing = teacher(1L, "9876543-2", "c@ipt.cl", ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicPeriodService.getOpenPeriodIds()).thenReturn(List.of());
        when(teacherRepository.save(existing)).thenReturn(existing);

        teacherService.changeStatus(1L, INACTIVE);

        verify(sectionRepository, never()).existsByTeacherIdAndAcademicPeriodIdIn(any(), any());
    }

    @Test
    void whenSetActive_thenNoChecksNeeded() {
        TeacherEntity existing = teacher(1L, "9876543-2", "c@ipt.cl", INACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.save(existing)).thenReturn(existing);

        assertThat(teacherService.changeStatus(1L, ACTIVE).getStatus()).isEqualTo(ACTIVE);
        verify(academicPeriodService, never()).getOpenPeriodIds();
    }
}
