package web.kplay.studentmanagement.domain.sms;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

@Entity
@Table(name = "sms_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category; // attendance, enrollment, payment, general

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String description;

    // 템플릿 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 템플릿 활성화
    public void activate() {
        this.isActive = true;
    }

    // 템플릿 수정
    public void update(String name, String category, String content, String description) {
        if (name != null) this.name = name;
        if (category != null) this.category = category;
        if (content != null) this.content = content;
        if (description != null) this.description = description;
    }
}
