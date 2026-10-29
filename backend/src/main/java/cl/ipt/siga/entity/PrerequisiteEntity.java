package cl.ipt.siga.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name="prerequisites",
        uniqueConstraints = {
                // Solo un registro para cada pareja asignatura:requisito
                @UniqueConstraint(name="ukprerequisite_subject_id_prerequisite_id",
                        columnNames = {"subject_id", "prerequisite_id"}
                )


        }
)
public class PrerequisiteEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long prerequisiteId;

}
