package cl.ipt.siga.service;

import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cl.ipt.siga.entity.enums.CareerStatus.ACTIVE;
import static cl.ipt.siga.entity.enums.CareerStatus.INACTIVE;
import static cl.ipt.siga.entity.enums.StudentStatus.*;
import static cl.ipt.siga.entity.enums.StudyPlanStatus.CURRENT;
import static cl.ipt.siga.service.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import cl.ipt.siga.security.KeycloakAdminClient;


@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CareerService careerService;

    @Mock
    private StudyPlanService studyPlanService;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private StudentService studentService;

    private StudentEntity newStudentRequest() {
        // RUN with dots and lowercase k, email with uppercase and spaces, status and plan sent by the client
        StudentEntity request = student(9L, "12.345.678-k", "  Ana.Perez@IPT.cl ", GRADUATED);
        request.setCareerId(1L);
        request.setStudyPlanId(999L);
        return request;
    }

    // ---------------------------------------------------------------- create

    @Test
    void whenCreateValidStudent_thenNormalizedRegularAndInCurrentPlan() {
        // Given
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", ACTIVE));
        when(studyPlanService.getCurrentByCareer(1L)).thenReturn(studyPlan(10L, 1L, "2021.3", CURRENT));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        StudentEntity created = studentService.create(newStudentRequest());

        // Then
        assertThat(created.getRun()).isEqualTo("12345678-K");
        assertThat(created.getEmail()).isEqualTo("ana.perez@ipt.cl");
        assertThat(created.getStatus()).isEqualTo(REGULAR);
        assertThat(created.getStudyPlanId()).isEqualTo(10L);
        assertThat(created.getId()).isNull();

        verify(keycloakAdminClient).createUser("ana.perez@ipt.cl", created.getFullName(), "12345678-K", "STUDENT");
    }

    @Test
    void whenCreateInInactiveCareer_thenRejected() {
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", INACTIVE));

        assertThatThrownBy(() -> studentService.create(newStudentRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void whenCreateWithInvalidRun_thenRejected() {
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", ACTIVE));
        StudentEntity request = newStudentRequest();
        request.setRun("abc");

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RUN");
    }

    @Test
    void whenCreateWithoutFullName_thenRejected() {
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", ACTIVE));
        StudentEntity request = newStudentRequest();
        request.setFullName("  ");

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Full name");
    }

    @Test
    void whenCreateWithExistingRun_thenConflict() {
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", ACTIVE));
        when(studentRepository.existsByRun("12345678-K")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(newStudentRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("RUN");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void whenCreateWithExistingEmail_thenConflict() {
        when(careerService.getById(1L)).thenReturn(career(1L, "10450", ACTIVE));
        when(studentRepository.existsByEmail("ana.perez@ipt.cl")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(newStudentRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
        verify(studentRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- queries

    @Test
    void whenGetAll_thenReturnsAllStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(student(1L, "12345678-K", "a@ipt.cl", REGULAR)));

        assertThat(studentService.getAll()).hasSize(1);
    }

    @Test
    void whenGetByCareer_thenChecksCareerAndReturnsItsStudents() {
        when(studentRepository.findByCareerId(1L)).thenReturn(List.of(student(1L, "12345678-K", "a@ipt.cl", REGULAR)));

        assertThat(studentService.getByCareer(1L)).hasSize(1);
        verify(careerService).getById(1L);
    }

    @Test
    void whenGetByIdMissing_thenNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- update

    @Test
    void whenUpdateKeepingRunAndEmail_thenUpdatesNameOnly() {
        // Given
        StudentEntity existing = student(1L, "12345678-K", "ana@ipt.cl", REGULAR);
        StudentEntity data = student(null, "12.345.678-k", "ANA@ipt.cl", GRADUATED);
        data.setFullName("Ana Pérez Soto");
        data.setCareerId(3L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        // When
        StudentEntity updated = studentService.update(1L, data);

        // Then: only personal data changes; status and career are never copied
        assertThat(updated.getFullName()).isEqualTo("Ana Pérez Soto");
        assertThat(updated.getStatus()).isEqualTo(REGULAR);
        assertThat(updated.getCareerId()).isEqualTo(1L);
        verify(studentRepository, never()).existsByRun(anyString());
        verify(studentRepository, never()).existsByEmail(anyString());
    }

    @Test
    void whenUpdateToRunOfAnotherStudent_thenConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "12345678-K", "ana@ipt.cl", REGULAR)));
        when(studentRepository.existsByRun("11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> studentService.update(1L, student(null, "11111111-1", "ana@ipt.cl", REGULAR)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void whenUpdateWithAnotherEmail_thenEmailIsKept() {
        StudentEntity existing = student(1L, "12345678-K", "ana@ipt.cl", REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        StudentEntity updated = studentService.update(1L, student(null, "12345678-K", "bob@ipt.cl", REGULAR));

        // The email links the student to the Keycloak account, so it is never changed
        assertThat(updated.getEmail()).isEqualTo("ana@ipt.cl");
    }

    // ---------------------------------------------------------------- status

    @Test
    void whenChangeStatusToPostponed_thenSaved() {
        StudentEntity existing = student(1L, "12345678-K", "ana@ipt.cl", REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        assertThat(studentService.changeStatus(1L, POSTPONED).getStatus()).isEqualTo(POSTPONED);
    }

    @Test
    void whenChangeStatusToGraduatedManually_thenRejected() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "12345678-K", "ana@ipt.cl", REGULAR)));

        assertThatThrownBy(() -> studentService.changeStatus(1L, GRADUATED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("automatically");
    }

    @Test
    void whenChangeStatusOfEliminatedStudent_thenRejected() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "12345678-K", "ana@ipt.cl", ELIMINATED)));

        assertThatThrownBy(() -> studentService.changeStatus(1L, REGULAR))
                .isInstanceOf(BusinessRuleException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void whenMarkAsGraduated_thenStatusSaved() {
        StudentEntity existing = student(1L, "12345678-K", "ana@ipt.cl", REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        studentService.markAsGraduated(1L);

        assertThat(existing.getStatus()).isEqualTo(GRADUATED);
        verify(studentRepository).save(existing);
    }

    @Test
    void whenMarkAsEliminated_thenStatusSaved() {
        StudentEntity existing = student(1L, "12345678-K", "ana@ipt.cl", REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        studentService.markAsEliminated(1L);

        assertThat(existing.getStatus()).isEqualTo(ELIMINATED);
        verify(studentRepository).save(existing);
    }


    @Test
    void whenGetByIds_thenReturnsThoseStudents() {
        when(studentRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                student(1L, "11111111-1", "ana@ipt.cl", REGULAR), student(2L, "22222222-2", "bob@ipt.cl", REGULAR)));

        assertThat(studentService.getByIds(List.of(1L, 2L))).hasSize(2);
    }
}
