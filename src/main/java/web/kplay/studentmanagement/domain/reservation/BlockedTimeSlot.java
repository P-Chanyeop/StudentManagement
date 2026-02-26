package web.kplay.studentmanagement.domain.reservation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "blocked_time_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BlockedTimeSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockType blockType; // SINGLE, RANGE, WEEKLY

    @Column
    private LocalDate blockDate; // SINGLE용

    @Column
    private LocalDate startDate; // RANGE용

    @Column
    private LocalDate endDate; // RANGE용

    @Enumerated(EnumType.STRING)
    @Column
    private DayOfWeek dayOfWeek; // WEEKLY용

    @Column(nullable = false)
    private LocalTime blockTime; // 차단 시간 (09:00, 10:00 등)

    @Column(length = 200)
    private String reason;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String targetType = "CLASS"; // CLASS, CONSULTATION

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public enum BlockType {
        SINGLE,  // 특정 날짜
        RANGE,   // 기간
        WEEKLY   // 매주 반복
    }

    public boolean isBlockedOn(LocalDate date) {
        if (!isActive) return false;
        return switch (blockType) {
            case SINGLE -> date.equals(blockDate);
            case RANGE -> !date.isBefore(startDate) && !date.isAfter(endDate);
            case WEEKLY -> date.getDayOfWeek() == dayOfWeek;
        };
    }

    public void deactivate() {
        this.isActive = false;
    }
}
