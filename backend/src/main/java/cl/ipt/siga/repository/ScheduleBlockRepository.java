package cl.ipt.siga.repository;

import cl.ipt.siga.entity.ScheduleBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlockEntity, Long> {
    List<ScheduleBlockEntity> findBySectionId(Long sectionId);
    List<ScheduleBlockEntity> findBySectionIdIn(List<Long> sectionIds);
}

