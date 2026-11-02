package cl.ipt.siga.service;

import static cl.ipt.siga.util.PersonDataUtils.*;
import cl.ipt.siga.entity.CareerEntity;
import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.StudentRepository;
import org.springframework.stereotype.Service;
import cl.ipt.siga.security.KeycloakAdminClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;
    private final KeycloakAdminClient keycloakAdminClient;

    public StudentService(StudentRepository studentRepository, CareerService careerService, StudyPlanService studyPlanService, KeycloakAdminClient keycloakAdminClient) {
        this.studentRepository = studentRepository;
        this.careerService = careerService;
        this.studyPlanService = studyPlanService;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    // Also creates the Keycloak account (initial password: the RUN). If Keycloak fails,
    // the transaction rolls back and the student is not saved either.
    @Transactional
    public StudentEntity create(StudentEntity student) {
        CareerEntity career = careerService.getById(student.getCareerId());   // 404
        if (career.getStatus() == CareerStatus.INACTIVE) {
            throw new BusinessRuleException("Career " + career.getCode() + " is inactive and cannot receive new students");
        }

        student.setRun(normalizeRun(student.getRun()));
        student.setEmail(normalizeEmail(student.getEmail()));
        student.setFullName(requireText(student.getFullName(), "Full name"));

        if (studentRepository.existsByRun(student.getRun())) {
            throw new DuplicateResourceException("A student with RUN " + student.getRun() + " already exists");
        }
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new DuplicateResourceException("A student with email " + student.getEmail() + " already exists");
        }

        // Assigned by the system, never taken from the request
        student.setStudyPlanId(studyPlanService.getCurrentByCareer(career.getId()).getId());
        student.setStatus(StudentStatus.REGULAR);
        student.setId(null);
        StudentEntity saved = studentRepository.save(student);
        keycloakAdminClient.createUser(saved.getEmail(), saved.getFullName(), saved.getRun(), "STUDENT");
        return saved;
    }

    public List<StudentEntity> getAll(){
        return studentRepository.findAll();
    }

    public List<StudentEntity> getByCareer(Long careerId) {
        careerService.getById(careerId);
        return studentRepository.findByCareerId(careerId);
    }


    public StudentEntity getById(Long id){
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student with that id not found"));
    }

    // Updates personal data only. Career, study plan and status have their own rules.
    public StudentEntity update(Long id, StudentEntity data) {
        StudentEntity student = getById(id);

        String newRun = normalizeRun(data.getRun());
        student.setFullName(requireText(data.getFullName(), "Full name"));

        if (!newRun.equals(student.getRun()) && studentRepository.existsByRun(newRun)) {
            throw new DuplicateResourceException("A student with RUN " + newRun + " already exists");
        }
        student.setRun(newRun);
        return studentRepository.save(student);
        // The email is not editable: it links the student to the Keycloak account.
    }


    public StudentEntity changeStatus(Long id, StudentStatus newStatus) {
        StudentEntity student = getById(id);

        if (newStatus == StudentStatus.GRADUATED || newStatus == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException("Status " + newStatus + " is assigned automatically at period closing");
        }
        // Design decision: a graduated or eliminated student cannot be changed manually
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException("A " + student.getStatus() + " student cannot change status manually");
        }

        student.setStatus(newStatus);
        return studentRepository.save(student);
    }


    // GRADUATED and ELIMINATED are assigned only by the period closing process,
// never through changeStatus
    public void markAsGraduated(Long id) {
        StudentEntity student = getById(id);
        student.setStatus(StudentStatus.GRADUATED);
        studentRepository.save(student);
    }

    public void markAsEliminated(Long id) {
        StudentEntity student = getById(id);
        student.setStatus(StudentStatus.ELIMINATED);
        studentRepository.save(student);
    }



    // The student linked to a Keycloak account: users are matched by email
    public StudentEntity getByEmail(String email) {
        if (email == null) {
            throw new ResourceNotFoundException("The logged-in user has no email");
        }
        return studentRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No student is linked to the account " + email));
    }

    // Several students at once, e.g. the students enrolled in a section
    public List<StudentEntity> getByIds(List<Long> ids) {
        return studentRepository.findAllById(ids);
    }
}
