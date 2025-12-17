package web.kplay.studentmanagement.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long scheduleId;
    private String courseName;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long enrollmentId;
    private ReservationStatus status;
    private String memo;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private String reservationSource;
    private Boolean canCancel;
}
