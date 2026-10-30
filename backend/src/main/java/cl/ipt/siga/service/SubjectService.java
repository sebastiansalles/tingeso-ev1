package cl.ipt.siga.service;


import cl.ipt.siga.entity.SubjectEntity;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.SubjectRepository;


import cl.ipt.siga.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final StudyPlanService studyPlanService;

    public SubjectService(SubjectRepository subjectRepository, StudyPlanService studyPlanService) {
        this.subjectRepository = subjectRepository;
        this.studyPlanService = studyPlanService;
    }

    private void validateSubjectData(SubjectEntity subjectEntity) {

        //we validate subjects semester value 
        if (subjectEntity.getSemester() < 1 || subjectEntity.getSemester() > 4) {
            throw new BusinessRuleException("Semester must be between 1 and 4");
        }
        //we calidate SCT: greater than 0 and max 7
        if (subjectEntity.getSct() < 1 || subjectEntity.getSct() > 7) {
            throw new BusinessRuleException("SCT must be between 1 and 7");
        }
        //TEL: each of the three hours not negativE, sum of the three, less than 8
        if (subjectEntity.getTheoryHours() < 0 || subjectEntity.getExerciseHours() < 0
                || subjectEntity.getLabHours() < 0) {
            throw new BusinessRuleException("TEL hours cannot be less than 0");
        }
        if (subjectEntity.getTheoryHours() + subjectEntity.getExerciseHours() + subjectEntity.getLabHours() > 7) {
            throw new BusinessRuleException("TEL total hours have to be less than 8");
        }
    }

    public List<SubjectEntity> getAll() {
        return subjectRepository.findAll();
    }

    public List<SubjectEntity> getByStudyPlan(Long studyPlanId) {
        // Validate study plan
        studyPlanService.getById(studyPlanId);

        // If studyplan exist, return all subjects
        return subjectRepository.findByStudyPlanId(studyPlanId);
    }

    public SubjectEntity getById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    public SubjectEntity create(SubjectEntity s) {
        // Verify study plan exists
        studyPlanService.getById(s.getStudyPlanId());

        // validate subject rules
        validateSubjectData(s);

        // validate code is not in use
        if (subjectRepository.existsByStudyPlanIdAndCode(s.getStudyPlanId(), s.getCode())) {
            throw new DuplicateResourceException("A subject with this code already exists");
        }

        // as usual, make sure no overwrite happens
        s.setId(null);
        return subjectRepository.save(s);
    }

    public SubjectEntity update(Long id, SubjectEntity data) {
        // get the subject to update and validate
        SubjectEntity existingSubject = getById(id);
        validateSubjectData(data);

        // check the new code doesnt exist
        if (!existingSubject.getCode().equals(data.getCode())) {
            if (subjectRepository.existsByStudyPlanIdAndCode(existingSubject.getStudyPlanId(), data.getCode())) {
                throw new DuplicateResourceException("A different subject with this code already exists");
            }
        }

        // Copy the new data
        existingSubject.setCode(data.getCode());
        existingSubject.setName(data.getName());
        existingSubject.setSemester(data.getSemester());
        existingSubject.setTheoryHours(data.getTheoryHours());
        existingSubject.setExerciseHours(data.getExerciseHours());
        existingSubject.setLabHours(data.getLabHours());
        existingSubject.setSct(data.getSct());

        return subjectRepository.save(existingSubject);
    }
}