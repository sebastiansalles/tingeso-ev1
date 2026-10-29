package cl.ipt.siga.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(
        name = "subjects",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_subject_study_plan_id_code",
                        columnNames = {"study_plan_id", "code"}

                )
        }
)

public class SubjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studyPlanId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    private int theoryHours;

    @Column(nullable = false)
    private int exerciseHours;

    @Column(nullable = false)
    private int labHours;

    @Column(nullable = false)
    private int sct;
}
