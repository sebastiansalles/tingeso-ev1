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
                //Una sección no podrá tener bloques horarios duplicados
                @UniqueConstraint(name= "uk_schedule_block_section_id_day_module", columnNames = {
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
    @Enumerated(EnumType.STRING)
    private Day day;

    @Column(nullable = false)
    private int module;

}
