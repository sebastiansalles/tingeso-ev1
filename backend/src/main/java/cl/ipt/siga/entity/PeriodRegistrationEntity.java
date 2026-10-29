package cl.ipt.siga.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name="period_registrations",
        uniqueConstraints = {
                //Un estudiante se matricula una sola vez por período
                @UniqueConstraint(name="period_registration_student_id_academic_period",columnNames = {
                        "student_id", "academic_period"
                })
        }
)

public class PeriodRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    Long academicPeriodId;
}
