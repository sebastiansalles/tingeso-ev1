package cl.ipt.siga.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name="sections",
        uniqueConstraints = {
                //Una asignatura podrá contar con una única sección durante un mismo período académico
                @UniqueConstraint(name="uk_section_subject_id_academic_period_id",
                        columnNames = {"subject_id", "academic_period_id"}
                )
        }
)
public class SectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private int capacity;
}
