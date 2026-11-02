package cl.ipt.siga.service;

import cl.ipt.siga.entity.TeacherEntity;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.security.KeycloakAdminClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cl.ipt.siga.util.PersonDataUtils.*;



@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final AcademicPeriodService academicPeriodService;
    private final KeycloakAdminClient keycloakAdminClient;

    public TeacherService(TeacherRepository teacherRepository, SectionRepository sectionRepository,  AcademicPeriodService academicPeriodService, KeycloakAdminClient keycloakAdminClient) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.academicPeriodService = academicPeriodService;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<TeacherEntity> getAll() {
        return teacherRepository.findAll();
    }

    public TeacherEntity getById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find a teacher with ID: " + id));
    }

    // Also creates the Keycloak account (initial password: the RUN). If Keycloak fails,
    // the transaction rolls back and the teacher is not saved either.
    @Transactional
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
        TeacherEntity saved = teacherRepository.save(teacher);
        keycloakAdminClient.createUser(saved.getEmail(), saved.getFullName(), saved.getRun(), "TEACHER");
        return saved;
    }

    // Updates personal and academic data. Status has its own method.
    public TeacherEntity update(Long id, TeacherEntity data) {
        TeacherEntity teacher = getById(id);

        String newRun = normalizeRun(data.getRun());

        if (!newRun.equals(teacher.getRun()) && teacherRepository.existsByRun(newRun)) {
            throw new DuplicateResourceException("A teacher with RUN " + newRun + " already exists");
        }


        teacher.setRun(newRun);
        teacher.setFullName(requireText(data.getFullName(), "Full name"));
        teacher.setProfessionalTitle(requireText(data.getProfessionalTitle(), "Professional title"));
        teacher.setAcademicDegree(requireText(data.getAcademicDegree(), "Academic degree"));
        return teacherRepository.save(teacher);
    }

    public TeacherEntity changeStatus(Long id, TeacherStatus newStatus) {
        TeacherEntity teacher = getById(id);
        if (newStatus == TeacherStatus.INACTIVE) {
            List<Long> openPeriodIds = academicPeriodService.getOpenPeriodIds();
            if (!openPeriodIds.isEmpty()
                    && sectionRepository.existsByTeacherIdAndAcademicPeriodIdIn(id, openPeriodIds)) {
                throw new BusinessRuleException("Teacher " + teacher.getFullName()
                        + " has sections in an open period and cannot be set as inactive");
            }
        }
        teacher.setStatus(newStatus);
        return teacherRepository.save(teacher);
    }





    // The student linked to a Keycloak account: users are matched by email
    public TeacherEntity getByEmail(String email) {
        if (email == null) {
            throw new ResourceNotFoundException("The logged-in user has no email");
        }
        return teacherRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No teacher is linked to the account " + email));
    }
}