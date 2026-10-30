package cl.ipt.siga.service;

import cl.ipt.siga.entity.PrerequisiteEntity;
import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.PrerequisiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PrerequisiteService {

    private static final int MAX_PREREQUISITES = 3;

    private final PrerequisiteRepository prerequisiteRepository;
    private final SubjectService subjectService;

    public PrerequisiteService(PrerequisiteRepository prerequisiteRepository,
                               SubjectService subjectService) {
        this.prerequisiteRepository = prerequisiteRepository;
        this.subjectService = subjectService;
    }

    public List<PrerequisiteEntity> getPrerequisites(Long subjectId) {
        subjectService.getById(subjectId);   // 404 if the subject does not exist
        return prerequisiteRepository.findBySubjectId(subjectId);
    }

    public PrerequisiteEntity addPrerequisite(Long subjectId, Long prerequisiteId) {
        SubjectEntity subject = subjectService.getById(subjectId);             // 404
        SubjectEntity prerequisite = subjectService.getById(prerequisiteId);   // 404

        if (!Objects.equals(subject.getStudyPlanId(), prerequisite.getStudyPlanId())) {
            throw new BusinessRuleException("A prerequisite must belong to the same study plan");
        }
        // Also prevents a subject from being its own prerequisite, and prevents cycles
        if (prerequisite.getSemester() >= subject.getSemester()) {
            throw new BusinessRuleException("A prerequisite must belong to an earlier semester");
        }
        if (prerequisiteRepository.countBySubjectId(subjectId) >= MAX_PREREQUISITES) {
            throw new BusinessRuleException(
                    "A subject cannot have more than " + MAX_PREREQUISITES + " prerequisites");
        }
        if (prerequisiteRepository.existsBySubjectIdAndPrerequisiteId(subjectId, prerequisiteId)) {
            throw new DuplicateResourceException(
                    "Subject " + prerequisite.getCode() + " is already a prerequisite of " + subject.getCode());
        }

        PrerequisiteEntity newPrerequisite = new PrerequisiteEntity();
        newPrerequisite.setSubjectId(subjectId);
        newPrerequisite.setPrerequisiteId(prerequisiteId);
        return prerequisiteRepository.save(newPrerequisite);
    }

    public void removePrerequisite(Long subjectId, Long prerequisiteId) {
        PrerequisiteEntity prerequisite = prerequisiteRepository
                .findBySubjectIdAndPrerequisiteId(subjectId, prerequisiteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject " + prerequisiteId + " is not a prerequisite of subject " + subjectId));
        prerequisiteRepository.delete(prerequisite);
    }
}