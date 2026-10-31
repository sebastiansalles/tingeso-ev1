package cl.ipt.siga.repository;

import cl.ipt.siga.entity.PeriodRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodRegistrationRepository extends JpaRepository<PeriodRegistrationEntity, Long> {
    boolean existsByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
    List<PeriodRegistrationEntity> findByAcademicPeriodId(Long academicPeriodId);
}