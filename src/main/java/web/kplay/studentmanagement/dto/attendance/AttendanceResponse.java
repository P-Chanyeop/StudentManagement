package web.kplay.studentmanagement.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long scheduleId;
    private String courseName;
    private AttendanceStatus status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalTime expectedLeaveTime;
    private String memo;
    private String reason;
}
