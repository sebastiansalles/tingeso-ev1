package cl.ipt.siga.service;

import cl.ipt.siga.entity.TeacherEntity;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static cl.ipt.siga.util.PersonDataUtils.*;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public List<TeacherEntity> getAll() {
        return teacherRepository.findAll();
    }

    public TeacherEntity getById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find a teacher with ID: " + id));
    }

    public TeacherEntity create(TeacherEntity teacher) {
        teacher.setRun(normalizeRun(teacher.getRun()));
        teacher.setEmail(normalizeEmail(teacher.getEmail()));
        teacher.setFullName(requireText(teacher.getFullName(), "Full name"));
        teacher.setProfessionalTitle(requireText(teacher.getProfessionalTitle(), "Professional title"));
        teacher.setAcademicDegree(requireText(teacher.getAcademicDegree(), "Academic degree"));

        if (teacherRepository.existsByRun(teacher.getRun())) {
            throw new DuplicateResourceException("A teacher with RUN " + teacher.getRun() + " already exists");
        }
        if (teacherRepository.existsByEmail(teacher.getEmail())) {
            throw new DuplicateResourceException("A teacher with email " + teacher.getEmail() + " already exists");
        }

        teacher.setStatus(TeacherStatus.ACTIVE);   // initial status, never taken from the request
        teacher.setId(null);
        return teacherRepository.save(teacher);
    }

    // Updates personal and academic data. Status has its own method.
    public TeacherEntity update(Long id, TeacherEntity data) {
        TeacherEntity teacher = getById(id);

        String newRun = normalizeRun(data.getRun());
        String newEmail = normalizeEmail(data.getEmail());

        if (!newRun.equals(teacher.getRun()) && teacherRepository.existsByRun(newRun)) {
            throw new DuplicateResourceException("A teacher with RUN " + newRun + " already exists");
        }
        if (!newEmail.equals(teacher.getEmail()) && teacherRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("A teacher with email " + newEmail + " already exists");
        }

        teacher.setRun(newRun);
        teacher.setEmail(newEmail);
        teacher.setFullName(requireText(data.getFullName(), "Full name"));
        teacher.setProfessionalTitle(requireText(data.getProfessionalTitle(), "Professional title"));
        teacher.setAcademicDegree(requireText(data.getAcademicDegree(), "Academic degree"));
        return teacherRepository.save(teacher);
    }

    public TeacherEntity changeStatus(Long id, TeacherStatus newStatus) {
        TeacherEntity teacher = getById(id);
        // reject INACTIVE if the teacher has sections in an OPEN period
        teacher.setStatus(newStatus);
        return teacherRepository.save(teacher);
    }
}