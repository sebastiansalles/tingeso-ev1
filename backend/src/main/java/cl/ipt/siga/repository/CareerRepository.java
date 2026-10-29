package cl.ipt.siga.repository;

import cl.ipt.siga.entity.CareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerRepository extends JpaRepository<CareerEntity, Long> {
    CareerEntity findByCareerId(Long careerId);
}