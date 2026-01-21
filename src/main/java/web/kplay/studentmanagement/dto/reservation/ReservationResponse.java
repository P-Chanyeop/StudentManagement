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
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Long enrollmentId;
    private ReservationStatus status;
    private String memo;
    private String consultationType;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private String reservationSource;
    private Boolean canCancel;
}
