package cl.ipt.siga.service;

import cl.ipt.siga.entity.StudyPlanEntity;
import cl.ipt.siga.entity.enums.StudyPlanStatus;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class StudyPlanService {
    private final StudyPlanRepository studyPlanRepository;
    private final CareerService careerService;

    public StudyPlanService(StudyPlanRepository studyPlanRepository, CareerRepository careerRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.careerService = new CareerService(careerRepository);
    }

    public List<StudyPlanEntity> getAll(){
        return studyPlanRepository.findAll();
    }

    public List<StudyPlanEntity> getByCareer(Long careerId) {
        List<StudyPlanEntity> studyPlans = studyPlanRepository.findByCareerId(careerId);

        if (studyPlans.isEmpty()) {
            throw new ResourceNotFoundException("Could not find any study plans for career ID: " + careerId);
        }

        return studyPlans;
    }


    public StudyPlanEntity getById(Long id){
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find a study plan with ID: "+ id));
    }

    @Transactional
    public StudyPlanEntity create(StudyPlanEntity plan) {
        // Check if career exists (if not: 404)
        careerService.getById(plan.getCareerId());

        // Check if there is no other plan with same identifier, if it is: 409
        if (studyPlanRepository.existsByCareerIdAndIdentifier(plan.getCareerId(), plan.getIdentifier())) {
            throw new DuplicateResourceException("A plan with this identifier already exists");
        }

        //If new plan is current, previous current goes NOT_CURRENT
        if (plan.getStatus() == StudyPlanStatus.CURRENT) {
            studyPlanRepository.findByCareerIdAndStatus(plan.getCareerId(), StudyPlanStatus.CURRENT)
                    .ifPresent(existingCurrentPlan -> {
                        existingCurrentPlan.setStatus(StudyPlanStatus.NOT_CURRENT);
                        studyPlanRepository.save(existingCurrentPlan);
                    });
        }

        // Make sure there is no overwrite to an existing id
        plan.setId(null);
        return studyPlanRepository.save(plan);
    }
}
