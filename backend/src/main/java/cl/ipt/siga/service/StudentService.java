package cl.ipt.siga.service;


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
import java.util.Objects;

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

    //NORMALIZATION METHODS
    // "12.345.678-k", "12345678-K" and "12345678k" all become "12345678-K"
    private String normalizeRun(String run) {
        if (run == null) {
            throw new BusinessRuleException("RUN is required");
        }
        String clean = run.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (!clean.matches("\\d{7,8}[0-9K]")) {
            throw new BusinessRuleException("Invalid RUN format: " + run);
        }
        // insert the hyphen before the check digit
        return clean.substring(0, clean.length() - 1) + "-" + clean.charAt(clean.length() - 1);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    public StudentEntity create(StudentEntity student) {
        CareerEntity career = careerService.getById(student.getCareerId());   // 404
        if (career.getStatus() == CareerStatus.INACTIVE) {
            throw new BusinessRuleException("Career " + career.getCode() + " is inactive and cannot receive new students");
        }

        student.setRun(normalizeRun(student.getRun()));
        student.setEmail(normalizeEmail(student.getEmail()));

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
        Long cId = careerService.getById(careerId).getId();
        return studentRepository.findByCareerId(careerId);
    }


    public StudentEntity getById(Long id){
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student with that id not found"));
    }

    public StudentEntity update(Long id, StudentEntity data){
        StudentEntity to_update = getById(id);
        if(!Objects.equals(data.getId(), to_update.getId())) {
            throw new BusinessRuleException("Student id cant be modified");
        }
        to_update.setCareerId(data.getCareerId());
        String normalizedEmail = normalizeEmail(data.getEmail());
        to_update.setEmail(normalizedEmail);
        String normalizedRun = normalizeRun(data.getRun());
        to_update.setRun(normalizedRun);
        to_update.setStudyPlanId(data.getStudyPlanId());
        to_update.setStatus(data.getStatus());
        return studentRepository.save(to_update);

    }


    public StudentEntity changeStatus(Long id, StudentStatus status){
        StudentEntity to_update = getById(id);
        to_update.setStatus(status);
        return studentRepository.save(to_update);

    }
}
