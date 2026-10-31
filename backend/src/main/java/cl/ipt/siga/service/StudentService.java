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

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;

    public StudentService(StudentRepository studentRepository, CareerService careerService, StudyPlanService studyPlanService) {
        this.studentRepository = studentRepository;
        this.careerService = careerService;
        this.studyPlanService = studyPlanService;
    }

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
        return studentRepository.save(student);
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
        String newEmail = normalizeEmail(data.getEmail());
        student.setFullName(requireText(data.getFullName(), "Full name"));

        if (!newRun.equals(student.getRun()) && studentRepository.existsByRun(newRun)) {
            throw new DuplicateResourceException("A student with RUN " + newRun + " already exists");
        }
        if (!newEmail.equals(student.getEmail()) && studentRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("A student with email " + newEmail + " already exists");
        }

        student.setRun(newRun);
        student.setFullName(data.getFullName());
        student.setEmail(newEmail);
        return studentRepository.save(student);
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
}
