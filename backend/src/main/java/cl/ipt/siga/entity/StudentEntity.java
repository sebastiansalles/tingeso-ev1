package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.StudentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;


@Entity
@Data
@NoArgsConstructor
@Table(
        name = "students"
)
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String run;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Long careerId;

    @Column(nullable = false)
    private Long studyPlanId;

    @Column(nullable = false)
    private StudentStatus status;
}
