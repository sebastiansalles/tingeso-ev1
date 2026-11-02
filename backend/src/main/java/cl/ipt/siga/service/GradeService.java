package cl.ipt.siga.service;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.entity.enums.SubjectResult;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class GradeService {

    private static final double MIN_GRADE = 1.0;
    private static final double MAX_GRADE = 7.0;
    private static final double PASSING_GRADE = 4.0;

    private final EnrollmentRepository enrollmentRepository;
    private final SectionService sectionService;
    private final AcademicPeriodService academicPeriodService;
    private final StudentService studentService;

    public GradeService(EnrollmentRepository enrollmentRepository,
                        SectionService sectionService,
                        AcademicPeriodService academicPeriodService,
                        StudentService studentService) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionService = sectionService;
        this.academicPeriodService = academicPeriodService;
        this.studentService = studentService;
    }

    // Students enrolled in a section (what the teacher sees to grade)
    public List<EnrollmentEntity> getSectionEnrollments(Long sectionId) {
        sectionService.getById(sectionId);   // 404
        return enrollmentRepository.findBySectionId(sectionId);
    }

    // Personal data of the students enrolled in a section (RUN, name, email), to show them to the teacher
    public List<StudentEntity> getSectionStudents(Long sectionId) {
        List<Long> studentIds = getSectionEnrollments(sectionId).stream()
                .map(EnrollmentEntity::getStudentId)
                .toList();
        return studentService.getByIds(studentIds);
    }

    // The ONLY place where finalGrade and result change, always together
    public EnrollmentEntity registerGrade(Long enrollmentId, Long teacherId, Double grade) {
        EnrollmentEntity enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find an enrollment with ID: " + enrollmentId));

        // Only the teacher responsible for the section can grade it.
        // The grade is attached to an enrollment of that section, so the student is necessarily enrolled in it.
        SectionEntity section = sectionService.getById(enrollment.getSectionId());
        if (!Objects.equals(section.getTeacherId(), teacherId)) {
            throw new BusinessRuleException("Only the teacher responsible for the section can register its grades");
        }

        // Grades can be registered or modified only while the period is OPEN
        academicPeriodService.requireOpen(enrollment.getAcademicPeriodId());

        if (grade == null || grade < MIN_GRADE || grade > MAX_GRADE) {
            throw new BusinessRuleException("The final grade must be between " + MIN_GRADE + " and " + MAX_GRADE);
        }

        enrollment.setFinalGrade(grade);
        enrollment.setResult(grade >= PASSING_GRADE ? SubjectResult.APPROVED : SubjectResult.FAILED);
        return enrollmentRepository.save(enrollment);
    }
}