package cl.ipt.siga.repository;

import cl.ipt.siga.entity.PrerequisiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrerequisiteRepository extends JpaRepository<PrerequisiteEntity, Long> {
    boolean existsBySubjectIdAndPrerequisiteId(Long subjectId, Long prerequisiteId);
    long countBySubjectId(Long subjectId);
    List<PrerequisiteEntity> findBySubjectId(Long subjectId);
    Optional<PrerequisiteEntity> findBySubjectIdAndPrerequisiteId(Long subjectId, Long prerequisiteId);

}
