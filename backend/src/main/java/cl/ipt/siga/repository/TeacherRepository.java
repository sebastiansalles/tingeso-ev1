package cl.ipt.siga.repository;

import cl.ipt.siga.entity.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);

}
