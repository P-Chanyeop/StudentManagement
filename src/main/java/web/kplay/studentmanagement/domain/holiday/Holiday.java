package web.kplay.studentmanagement.domain.holiday;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, length = 100)
    private String name; // 공휴일 명칭 (예: 설날, 추석, 어린이날 등)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRecurring = false; // 매년 반복 여부

    @Column(length = 500)
    private String description;

    // 공휴일 정보 업데이트
    public void updateInfo(String name, Boolean isRecurring, String description) {
        this.name = name;
        this.isRecurring = isRecurring;
        this.description = description;
    }
}
