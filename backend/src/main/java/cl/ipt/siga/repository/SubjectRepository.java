package cl.ipt.siga.repository;

import cl.ipt.siga.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<SubjectEntity,Long>{

    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);
    List<SubjectEntity> findByStudyPlanId(Long studyPlanId);
}