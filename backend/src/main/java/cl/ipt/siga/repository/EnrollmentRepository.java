package cl.ipt.siga.repository;

import cl.ipt.siga.entity.EnrollmentEntity;
import cl.ipt.siga.entity.enums.SubjectResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    List<EnrollmentEntity> findByStudentId(Long studentId);
    List<EnrollmentEntity> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
    List<EnrollmentEntity> findByStudentIdAndResult(Long studentId, SubjectResult result);
    long countBySectionId(Long sectionId);
}