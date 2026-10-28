package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.TeacherStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;


@Entity
@Data
@NoArgsConstructor
@Table(
        name="teachers"
)
public class TeacherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique=true)
    private String run;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique=true)
    private String email;

    @Column(nullable = false)
    private String professionalTitle;

    @Column(nullable = false)
    private String academicDegree;

    @Column(nullable = false)
    private TeacherStatus status;
}
