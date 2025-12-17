package web.kplay.studentmanagement.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCheckInRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "스케줄 ID는 필수입니다")
    private Long scheduleId;

    // 예상 하원 시간 (없으면 등원 시간 + 수업 시간(분)으로 자동 계산)
    private LocalTime expectedLeaveTime;
}
