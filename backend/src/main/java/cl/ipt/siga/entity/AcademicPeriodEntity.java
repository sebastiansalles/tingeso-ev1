package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.PeriodStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name="academic_periods",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_academic_period_year_semester",
                        columnNames = {"year","semester"}
                )
        }
)

public class AcademicPeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PeriodStatus status;
}
