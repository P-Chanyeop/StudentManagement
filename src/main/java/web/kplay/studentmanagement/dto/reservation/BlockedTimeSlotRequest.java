package web.kplay.studentmanagement.dto.reservation;

import lombok.*;
import web.kplay.studentmanagement.domain.reservation.BlockedTimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedTimeSlotRequest {
    private BlockedTimeSlot.BlockType blockType;
    private LocalDate blockDate;      // SINGLE
    private LocalDate startDate;      // RANGE
    private LocalDate endDate;        // RANGE
    private DayOfWeek dayOfWeek;      // WEEKLY
    private LocalTime blockTime;
    private String reason;
    private String targetType;
}
