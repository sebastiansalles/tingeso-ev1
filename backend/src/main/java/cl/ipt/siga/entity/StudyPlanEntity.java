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
                @UniqueConstraint(name = "uk_sp_identifier",
                        columnNames = {"id", "identifier"}
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
    private StudyPlanStatus status;
}
