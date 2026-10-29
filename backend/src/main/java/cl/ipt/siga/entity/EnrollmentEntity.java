package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.SubjectResult;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDate;


@Entity
@Data
@NoArgsConstructor
@Table(
        name="enrollments",
        uniqueConstraints = {
                //Una asignatura no podrá ser inscrita más de una vez por el mismo
                //estudiante durante un mismo período académico.
                @UniqueConstraint(
                        name="uk_enrollment_student_id_subject_id_academic_period_id",
                        columnNames = {"student_id", "subject_id", "academic_period_id"}
                )
        }
)
public class EnrollmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private LocalDate enrollmentDate;

    //"Las modificaciones realizadas sobre una asignatura no deberán alterar la información registrada
    //previamente en el historial académico de los estudiantes"
    //We save a copy of this data so they are not altered in the future
    @Column(nullable = false)
    private String subjectCode;
    @Column(nullable = false)
    private String subjectName;
    @Column(nullable = false)
    private int subjectSct;

    //Grade and result can be null so they can be updated later
    @Column
    private Double finalGrade;
    @Column
    @Enumerated(EnumType.STRING)
    private SubjectResult result;

}
