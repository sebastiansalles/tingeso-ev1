package cl.ipt.siga.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name="prerequisites")
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
