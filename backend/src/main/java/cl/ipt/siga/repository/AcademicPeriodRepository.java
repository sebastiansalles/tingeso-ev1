package cl.ipt.siga.repository;

import cl.ipt.siga.entity.AcademicPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, Long> {
    boolean existsByYearAndSemester(int year, int semester);
}
