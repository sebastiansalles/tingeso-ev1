package cl.ipt.siga.repository;

import cl.ipt.siga.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);
    List<StudentEntity> findByCareerId(Long careerId);
}
