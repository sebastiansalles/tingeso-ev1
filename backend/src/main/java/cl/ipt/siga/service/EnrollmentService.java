package cl.ipt.siga.service;

import cl.ipt.siga.entity.*;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.entity.enums.SubjectResult;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final int MIN_SUBJECTS = 3;
    private static final int MAX_SUBJECTS = 6;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;
    private final PeriodRegistrationService periodRegistrationService;
    private final SubjectService subjectService;
    private final PrerequisiteService prerequisiteService;
    private final SectionService sectionService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentService studentService,
                             AcademicPeriodService academicPeriodService,
                             PeriodRegistrationService periodRegistrationService,
                             SubjectService subjectService,
                             PrerequisiteService prerequisiteService,
                             SectionService sectionService) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentService = studentService;
        this.academicPeriodService = academicPeriodService;
        this.periodRegistrationService = periodRegistrationService;
        this.subjectService = subjectService;
        this.prerequisiteService = prerequisiteService;
        this.sectionService = sectionService;
    }

    // ==================== QUERIES ====================

    public EnrollmentEntity getById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find an enrollment with ID: " + id));
    }

    // All enrollments of a student, optionally filtered by period
    public List<EnrollmentEntity> getByStudent(Long studentId, Long academicPeriodId) {
        studentService.getById(studentId);   // 404
        if (academicPeriodId == null) {
            return enrollmentRepository.findByStudentId(studentId);
        }
        academicPeriodService.getById(academicPeriodId);   // 404
        return enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, academicPeriodId);
    }

    // Academic history: enrollments of CLOSED periods, whose grades and results are final
    public List<EnrollmentEntity> getAcademicHistory(Long studentId) {
        studentService.getById(studentId);   // 404
        List<Long> openPeriodIds = academicPeriodService.getOpenPeriodIds();
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> !openPeriodIds.contains(e.getAcademicPeriodId()))
                .toList();
    }

    // Subjects the student can still enroll in this period (what the enrollment screen shows)
    public List<SubjectEntity> getAvailableSubjects(Long studentId, Long academicPeriodId) {
        StudentEntity student = studentService.getById(studentId);   // 404
        academicPeriodService.getById(academicPeriodId);              // 404
        Set<Long> enrolledNow = enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, academicPeriodId)
                .stream().map(EnrollmentEntity::getSubjectId).collect(Collectors.toSet());

        return findAvailableSubjects(student, academicPeriodId, getApprovedSubjectIds(studentId)).stream()
                .filter(s -> !enrolledNow.contains(s.getId()))
                .toList();
    }

    // ==================== ENROLLMENT ====================

    @Transactional   // all requested subjects are enrolled, or none
    public List<EnrollmentEntity> enroll(Long studentId, Long academicPeriodId, List<Long> subjectIds) {
        // Rules 1-3: regular student, open period, registered in the period
        StudentEntity student = studentService.getById(studentId);
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessRuleException("Only REGULAR students can enroll (current status: " + student.getStatus() + ")");
        }
        academicPeriodService.requireOpen(academicPeriodId);
        if (!periodRegistrationService.isRegistered(studentId, academicPeriodId)) {
            throw new BusinessRuleException("Student " + student.getRun() + " is not registered in this academic period");
        }

        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BusinessRuleException("At least one subject must be selected");
        }
        Set<Long> requestedIds = new LinkedHashSet<>(subjectIds);
        if (requestedIds.size() != subjectIds.size()) {
            throw new BusinessRuleException("The same subject was selected more than once");
        }

        List<EnrollmentEntity> current = enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, academicPeriodId);
        Set<Long> currentSubjectIds = current.stream().map(EnrollmentEntity::getSubjectId).collect(Collectors.toSet());

        Map<Long, SubjectEntity> planSubjects = subjectService.getByStudyPlan(student.getStudyPlanId()).stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
        Set<Long> approvedIds = getApprovedSubjectIds(studentId);

        // Rules 4-7, subject by subject. Remembers the section of each requested subject.
        Map<Long, SectionEntity> newSections = new LinkedHashMap<>();
        for (Long subjectId : requestedIds) {
            SubjectEntity subject = planSubjects.get(subjectId);
            if (subject == null) {
                throw new BusinessRuleException("Subject " + subjectId + " does not belong to the student's study plan");
            }
            if (currentSubjectIds.contains(subjectId)) {
                throw new DuplicateResourceException("Subject " + subject.getCode() + " is already enrolled in this period");
            }
            if (approvedIds.contains(subjectId)) {
                throw new BusinessRuleException("Subject " + subject.getCode() + " is already approved");
            }
            for (PrerequisiteEntity prerequisite : prerequisiteService.getPrerequisites(subjectId)) {
                if (!approvedIds.contains(prerequisite.getPrerequisiteId())) {
                    SubjectEntity missing = planSubjects.get(prerequisite.getPrerequisiteId());
                    String missingCode = missing != null ? missing.getCode() : String.valueOf(prerequisite.getPrerequisiteId());
                    throw new BusinessRuleException("Subject " + subject.getCode() + " requires " + missingCode + " to be approved");
                }
            }
            SectionEntity section = sectionService.findBySubjectAndPeriod(subjectId, academicPeriodId)
                    .orElseThrow(() -> new BusinessRuleException("Subject " + subject.getCode() + " is not offered in this period"));
            newSections.put(subjectId, section);
        }

        // Rule 8: between 3 and 6 per period (total, counting what is already enrolled).
        // If fewer than 3 subjects are available, the minimum is what is available.
        int total = current.size() + requestedIds.size();
        if (total > MAX_SUBJECTS) {
            throw new BusinessRuleException("At most " + MAX_SUBJECTS + " subjects per period (total would be " + total + ")");
        }
        List<SubjectEntity> available = findAvailableSubjects(student, academicPeriodId, approvedIds);
        int minRequired = Math.min(MIN_SUBJECTS, available.size());
        if (total < minRequired) {
            throw new BusinessRuleException("At least " + minRequired + " subjects per period (total would be " + total + ")");
        }

        // Rule 9: no higher semester while an available subject of a lower semester is left out
        Set<Long> chosenIds = new HashSet<>(currentSubjectIds);
        chosenIds.addAll(requestedIds);
        int highestChosenSemester = chosenIds.stream()
                .map(planSubjects::get).filter(Objects::nonNull)
                .mapToInt(SubjectEntity::getSemester).max().orElse(0);
        for (SubjectEntity pending : available) {
            if (!chosenIds.contains(pending.getId()) && pending.getSemester() < highestChosenSemester) {
                throw new BusinessRuleException("Subject " + pending.getCode() + " (semester " + pending.getSemester()
                        + ") is pending and available; it must be enrolled before subjects of semester " + highestChosenSemester);
            }
        }

        // Rule 10: seats
        for (Map.Entry<Long, SectionEntity> entry : newSections.entrySet()) {
            SectionEntity section = entry.getValue();
            if (enrollmentRepository.countBySectionId(section.getId()) >= section.getCapacity()) {
                throw new BusinessRuleException("The section of " + planSubjects.get(entry.getKey()).getCode() + " has no available seats");
            }
        }

        // Rule 11: no schedule conflicts among all sections of the period (already enrolled + new)
        Map<String, String> occupied = new HashMap<>();   // "L2" -> subject code
        for (EnrollmentEntity enrollment : current) {
            occupyBlocks(occupied, enrollment.getSectionId(), enrollment.getSubjectCode());
        }
        for (Map.Entry<Long, SectionEntity> entry : newSections.entrySet()) {
            occupyBlocks(occupied, entry.getValue().getId(), planSubjects.get(entry.getKey()).getCode());
        }

        // Everything is valid: create the enrollments, with a copy of the subject data for the history
        LocalDate today = LocalDate.now();
        List<EnrollmentEntity> created = new ArrayList<>();
        for (Map.Entry<Long, SectionEntity> entry : newSections.entrySet()) {
            SubjectEntity subject = planSubjects.get(entry.getKey());
            EnrollmentEntity enrollment = new EnrollmentEntity();
            enrollment.setStudentId(studentId);
            enrollment.setSubjectId(subject.getId());
            enrollment.setSectionId(entry.getValue().getId());
            enrollment.setAcademicPeriodId(academicPeriodId);
            enrollment.setEnrollmentDate(today);
            enrollment.setSubjectCode(subject.getCode());
            enrollment.setSubjectName(subject.getName());
            enrollment.setSubjectSct(subject.getSct());
            created.add(enrollment);   // finalGrade and result stay null until the teacher grades
        }
        return enrollmentRepository.saveAll(created);
    }

    // Cancellation frees the seat (seats are counted, not stored)
    public void cancel(Long enrollmentId) {
        EnrollmentEntity enrollment = getById(enrollmentId);                 // 404
        academicPeriodService.requireOpen(enrollment.getAcademicPeriodId()); // only while OPEN
        // Design decision: once graded, an enrollment cannot be cancelled
        if (enrollment.getFinalGrade() != null) {
            throw new BusinessRuleException("An enrollment with a registered final grade cannot be cancelled");
        }
        enrollmentRepository.delete(enrollment);
    }

    // ==================== HELPERS ====================

    // Approved = APPROVED result in a CLOSED period (grades of open periods can still change)
    private Set<Long> getApprovedSubjectIds(Long studentId) {
        List<Long> openPeriodIds = academicPeriodService.getOpenPeriodIds();
        return enrollmentRepository.findByStudentIdAndResult(studentId, SubjectResult.APPROVED).stream()
                .filter(e -> !openPeriodIds.contains(e.getAcademicPeriodId()))
                .map(EnrollmentEntity::getSubjectId)
                .collect(Collectors.toSet());
    }

    // Available = not approved + offered in the period + all prerequisites approved
    private List<SubjectEntity> findAvailableSubjects(StudentEntity student, Long academicPeriodId, Set<Long> approvedIds) {
        List<SubjectEntity> available = new ArrayList<>();
        for (SubjectEntity subject : subjectService.getByStudyPlan(student.getStudyPlanId())) {
            if (approvedIds.contains(subject.getId())) {
                continue;
            }
            if (sectionService.findBySubjectAndPeriod(subject.getId(), academicPeriodId).isEmpty()) {
                continue;
            }
            boolean prerequisitesApproved = prerequisiteService.getPrerequisites(subject.getId()).stream()
                    .allMatch(p -> approvedIds.contains(p.getPrerequisiteId()));
            if (prerequisitesApproved) {
                available.add(subject);
            }
        }
        return available;
    }

    // Marks the blocks of a section as occupied; fails if one is already taken by another subject
    private void occupyBlocks(Map<String, String> occupied, Long sectionId, String subjectCode) {
        for (ScheduleBlockEntity block : sectionService.getSchedule(sectionId)) {
            String key = "" + block.getDay().getCode() + block.getModule();
            String other = occupied.putIfAbsent(key, subjectCode);
            if (other != null) {
                throw new BusinessRuleException("Schedule conflict at " + key + " between " + other + " and " + subjectCode);
            }
        }
    }
}