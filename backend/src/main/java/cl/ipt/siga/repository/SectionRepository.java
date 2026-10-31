package cl.ipt.siga.repository;

import cl.ipt.siga.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    boolean existsBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);
    List<SectionEntity> findByAcademicPeriodId(Long academicPeriodId);
    List<SectionEntity> findByTeacherIdAndAcademicPeriodId(Long teacherId, Long academicPeriodId);
    boolean existsByTeacherIdAndAcademicPeriodIdIn(Long teacherId, List<Long> academicPeriodIds);
}
