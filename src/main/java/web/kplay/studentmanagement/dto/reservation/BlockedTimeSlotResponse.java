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
public class BlockedTimeSlotResponse {
    private Long id;
    private BlockedTimeSlot.BlockType blockType;
    private LocalDate blockDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private DayOfWeek dayOfWeek;
    private LocalTime blockTime;
    private String reason;
    private Boolean isActive;
    private String targetType;

    public static BlockedTimeSlotResponse from(BlockedTimeSlot entity) {
        return BlockedTimeSlotResponse.builder()
                .id(entity.getId())
                .blockType(entity.getBlockType())
                .blockDate(entity.getBlockDate())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .dayOfWeek(entity.getDayOfWeek())
                .blockTime(entity.getBlockTime())
                .reason(entity.getReason())
                .isActive(entity.getIsActive())
                .targetType(entity.getTargetType())
                .build();
    }
}
