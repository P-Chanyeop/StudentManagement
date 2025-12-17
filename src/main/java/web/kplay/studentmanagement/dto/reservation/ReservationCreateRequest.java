package web.kplay.studentmanagement.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "스케줄 ID는 필수입니다")
    private Long scheduleId;

    private Long enrollmentId;

    private String memo;

    private String reservationSource; // WEB, NAVER, PHONE 등
}
