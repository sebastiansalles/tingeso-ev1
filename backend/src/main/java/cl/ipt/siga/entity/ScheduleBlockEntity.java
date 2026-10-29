package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.Day;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
        name="schedule_blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {
                        "section_id", "day", "module"
                })
        }
)
public class ScheduleBlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private Day day;

    @Column(nullable = false)
    private int module;

}
