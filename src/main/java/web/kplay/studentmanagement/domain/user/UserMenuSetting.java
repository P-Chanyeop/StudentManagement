package web.kplay.studentmanagement.domain.user;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

@Entity
@Table(name = "user_menu_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserMenuSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String menuOrder; // JSON 형태로 메뉴 순서 저장

    public void updateMenuOrder(String menuOrder) {
        this.menuOrder = menuOrder;
    }
}
