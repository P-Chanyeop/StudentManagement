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

    // 예상 하원 시간 (없으면 수업 종료 시간으로 자동 설정)
    private LocalTime expectedLeaveTime;
}
