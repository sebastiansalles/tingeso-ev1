package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.StudentEntity;
import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.entity.enums.SubjectResult;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PeriodClosingService {

    private final AcademicPeriodService academicPeriodService;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;

    public PeriodClosingService(AcademicPeriodService academicPeriodService,
                                EnrollmentRepository enrollmentRepository,
                                StudentService studentService,
                                SubjectService subjectService) {
        this.academicPeriodService = academicPeriodService;
        this.enrollmentRepository = enrollmentRepository;
        this.studentService = studentService;
        this.subjectService = subjectService;
    }

    @Transactional   // statuses and period are updated together, or not at all
    public AcademicPeriodEntity close(Long academicPeriodId) {
        // Only an OPEN period can be closed
        AcademicPeriodEntity period = academicPeriodService.requireOpen(academicPeriodId);

        // Every enrollment must have a final grade; otherwise report the incomplete sections
        List<EnrollmentEntity> periodEnrollments = enrollmentRepository.findByAcademicPeriodId(academicPeriodId);
        List<String> incomplete = periodEnrollments.stream()
                .filter(e -> e.getFinalGrade() == null)
                .map(EnrollmentEntity::getSubjectCode)
                .distinct().sorted().toList();
        if (!incomplete.isEmpty()) {
            throw new BusinessRuleException("The period cannot be closed. Sections with missing grades: "
                    + String.join(", ", incomplete));
        }

        // Update the academic status of every student enrolled in the period
        List<Long> openPeriodIds = academicPeriodService.getOpenPeriodIds();
        Set<Long> studentIds = periodEnrollments.stream()
                .map(EnrollmentEntity::getStudentId).collect(Collectors.toSet());

        for (Long studentId : studentIds) {
            // Final records = closed periods + the period being closed now
            List<EnrollmentEntity> finalRecords = enrollmentRepository.findByStudentId(studentId).stream()
                    .filter(e -> e.getAcademicPeriodId().equals(academicPeriodId)
                            || !openPeriodIds.contains(e.getAcademicPeriodId()))
                    .toList();

            if (failedSameSubjectTwice(finalRecords)) {
                studentService.markAsEliminated(studentId);
            } else if (approvedWholePlan(studentId, finalRecords)) {
                studentService.markAsGraduated(studentId);
            }
        }

        // Results become history: the period is closed and can no longer be modified
        return academicPeriodService.markAsClosed(period);
    }

    // ELIMINATED: the same subject was failed twice
    private boolean failedSameSubjectTwice(List<EnrollmentEntity> records) {
        Map<Long, Long> failuresBySubject = records.stream()
                .filter(e -> e.getResult() == SubjectResult.FAILED)
                .collect(Collectors.groupingBy(EnrollmentEntity::getSubjectId, Collectors.counting()));
        return failuresBySubject.values().stream().anyMatch(count -> count >= 2);
    }

    // GRADUATED: every subject of the student's study plan is approved
    private boolean approvedWholePlan(Long studentId, List<EnrollmentEntity> records) {
        StudentEntity student = studentService.getById(studentId);
        List<SubjectEntity> planSubjects = subjectService.getByStudyPlan(student.getStudyPlanId());
        if (planSubjects.isEmpty()) {
            return false;   // a plan without subjects cannot be "completed"
        }
        Set<Long> approved = records.stream()
                .filter(e -> e.getResult() == SubjectResult.APPROVED)
                .map(EnrollmentEntity::getSubjectId)
                .collect(Collectors.toSet());
        return planSubjects.stream().allMatch(s -> approved.contains(s.getId()));
    }
}