package cl.ipt.siga.service;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.entity.ScheduleBlockEntity;
import cl.ipt.siga.entity.SectionEntity;
import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.entity.TeacherEntity;
import cl.ipt.siga.entity.enums.Day;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.ScheduleBlockRepository;
import cl.ipt.siga.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SectionService {

    private static final int MAX_CAPACITY = 60;
    private static final int MIN_MODULE = 1;
    private static final int MAX_MODULE = 6;
    private static final int HOURS_PER_BLOCK = 2;

    private final SectionRepository sectionRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SubjectService subjectService;
    private final AcademicPeriodService academicPeriodService;
    private final TeacherService teacherService;

    public SectionService(SectionRepository sectionRepository,
                          ScheduleBlockRepository scheduleBlockRepository,
                          SubjectService subjectService,
                          AcademicPeriodService academicPeriodService,
                          TeacherService teacherService) {
        this.sectionRepository = sectionRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.subjectService = subjectService;
        this.academicPeriodService = academicPeriodService;
        this.teacherService = teacherService;
    }

    public List<SectionEntity> getAll() {
        return sectionRepository.findAll();
    }

    public List<SectionEntity> getByAcademicPeriod(Long academicPeriodId) {
        academicPeriodService.getById(academicPeriodId);   // 404
        return sectionRepository.findByAcademicPeriodId(academicPeriodId);
    }

    public SectionEntity getById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find a section with ID: " + id));
    }

    public List<ScheduleBlockEntity> getSchedule(Long sectionId) {
        getById(sectionId);   // 404
        return scheduleBlockRepository.findBySectionId(sectionId);
    }

    @Transactional   // section + blocks are saved together, or not at all
    public SectionEntity create(SectionRequest request) {
        // Rules 1-3: subject, open period and active teacher
        SubjectEntity subject = subjectService.getById(request.subjectId());
        academicPeriodService.requireOpen(request.academicPeriodId());
        TeacherEntity teacher = teacherService.getById(request.teacherId());
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Teacher " + teacher.getFullName() + " is not active");
        }

        // Rule 4: capacity
        if (request.capacity() < 1 || request.capacity() > MAX_CAPACITY) {
            throw new BusinessRuleException("Capacity must be between 1 and " + MAX_CAPACITY);
        }

        // Rule 5: one section per subject and period
        if (sectionRepository.existsBySubjectIdAndAcademicPeriodId(subject.getId(), request.academicPeriodId())) {
            throw new DuplicateResourceException("Subject " + subject.getCode() + " already has a section in this period");
        }

        // Rules 6-7: parse "L2", "W3"... into blocks, rejecting invalid or repeated ones
        List<ScheduleBlockEntity> blocks = parseSchedule(request.schedule());

        // Rule 8: number of blocks = TEL / 2
        int tel = subject.getTheoryHours() + subject.getExerciseHours() + subject.getLabHours();
        int requiredBlocks = tel / HOURS_PER_BLOCK;
        if (blocks.size() != requiredBlocks) {
            throw new BusinessRuleException("Subject " + subject.getCode() + " has TEL " + tel
                    + " and needs exactly " + requiredBlocks + " schedule blocks, but " + blocks.size() + " were given");
        }

        // Rule 9: the teacher cannot be in two places at the same time
        checkTeacherScheduleConflict(teacher, request.academicPeriodId(), blocks);

        // Everything is valid: save the section, then its blocks
        SectionEntity section = new SectionEntity();
        section.setSubjectId(subject.getId());
        section.setAcademicPeriodId(request.academicPeriodId());
        section.setTeacherId(teacher.getId());
        section.setCapacity(request.capacity());
        SectionEntity saved = sectionRepository.save(section);

        for (ScheduleBlockEntity block : blocks) {
            block.setSectionId(saved.getId());
        }
        scheduleBlockRepository.saveAll(blocks);

        return saved;
    }

    // "L2" -> MONDAY, module 2. Rejects empty schedules, invalid codes and repeated blocks.
    private List<ScheduleBlockEntity> parseSchedule(List<String> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            throw new BusinessRuleException("A section needs at least one schedule block");
        }

        List<ScheduleBlockEntity> blocks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String code : schedule) {
            if (code == null || !code.trim().matches("[A-Za-z]\\d")) {
                throw new BusinessRuleException("Invalid schedule block: " + code + " (expected format like L2)");
            }
            String clean = code.trim().toUpperCase();
            Day day = Day.fromCode(clean.charAt(0));
            int module = Character.getNumericValue(clean.charAt(1));
            if (module < MIN_MODULE || module > MAX_MODULE) {
                throw new BusinessRuleException("Invalid module in " + clean + " (valid: 1 to 6)");
            }
            if (!seen.add(blockKey(day, module))) {
                throw new BusinessRuleException("Schedule block " + clean + " is repeated");
            }

            ScheduleBlockEntity block = new ScheduleBlockEntity();
            block.setDay(day);
            block.setModule(module);
            blocks.add(block);
        }
        return blocks;
    }

    private void checkTeacherScheduleConflict(TeacherEntity teacher, Long academicPeriodId,
                                              List<ScheduleBlockEntity> newBlocks) {
        // Sections this teacher already has in the same period
        List<Long> teacherSectionIds = sectionRepository
                .findByTeacherIdAndAcademicPeriodId(teacher.getId(), academicPeriodId)
                .stream().map(SectionEntity::getId).toList();
        if (teacherSectionIds.isEmpty()) {
            return;   // no other sections, no possible conflict
        }

        Set<String> newKeys = new HashSet<>();
        for (ScheduleBlockEntity block : newBlocks) {
            newKeys.add(blockKey(block.getDay(), block.getModule()));
        }

        for (ScheduleBlockEntity existing : scheduleBlockRepository.findBySectionIdIn(teacherSectionIds)) {
            String key = blockKey(existing.getDay(), existing.getModule());
            if (newKeys.contains(key)) {
                throw new DuplicateResourceException("Teacher " + teacher.getFullName()
                        + " already has a section at " + key + " in this period");
            }
        }
    }

    // MONDAY + 2 -> "L2"
    private String blockKey(Day day, int module) {
        return "" + day.getCode() + module;
    }

    // The section offered for a subject in a period, if any (one per subject and period)
    public Optional<SectionEntity> findBySubjectAndPeriod(Long subjectId, Long academicPeriodId) {
        return sectionRepository.findBySubjectIdAndAcademicPeriodId(subjectId, academicPeriodId);
    }


    // Sections assigned to a teacher, optionally filtered by period
    public List<SectionEntity> getByTeacher(Long teacherId, Long academicPeriodId) {
        teacherService.getById(teacherId);   // 404
        if (academicPeriodId == null) {
            return sectionRepository.findByTeacherId(teacherId);
        }
        academicPeriodService.getById(academicPeriodId);   // 404
        return sectionRepository.findByTeacherIdAndAcademicPeriodId(teacherId, academicPeriodId);
    }
}