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
                @UniqueConstraint(
                        columnNames = {"subject_id", "academic_period"}
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
