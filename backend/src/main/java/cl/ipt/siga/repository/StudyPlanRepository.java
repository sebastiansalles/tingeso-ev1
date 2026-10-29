package cl.ipt.siga.repository;

import cl.ipt.siga.entity.StudyPlanEntity;
import cl.ipt.siga.entity.enums.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPlanRepository  extends JpaRepository<StudyPlanEntity, Long> {
    boolean existsByCareerIdAndIdentifier(Long careerId, String identifier);

    Optional<StudyPlanEntity> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);

    List<StudyPlanEntity> findByCareerId(Long careerId);

}
