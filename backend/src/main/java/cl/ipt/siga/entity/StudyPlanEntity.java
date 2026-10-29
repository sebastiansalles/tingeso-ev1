package cl.ipt.siga.entity;
import cl.ipt.siga.entity.enums.StudyPlanStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name = "study_plans",
        uniqueConstraints = {
                // El identificador del plan es unico por carrera
                @UniqueConstraint(name = "uk_study_plan_career_id_identifier",
                        columnNames = {"career_id", "identifier"}
                )
        }
)

public class StudyPlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long careerId;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyPlanStatus status;
}
