package cl.ipt.siga.repository;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import cl.ipt.siga.entity.enums.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, Long> {
    boolean existsByYearAndSemester(int year, int semester);
    List<AcademicPeriodEntity> findByStatus(PeriodStatus status);
}

