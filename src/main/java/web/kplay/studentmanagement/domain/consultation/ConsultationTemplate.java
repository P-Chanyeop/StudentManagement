package web.kplay.studentmanagement.domain.consultation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import web.kplay.studentmanagement.domain.BaseEntity;

@Entity
@Table(name = "consultation_templates")
@Getter @Setter
@NoArgsConstructor
public class ConsultationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
