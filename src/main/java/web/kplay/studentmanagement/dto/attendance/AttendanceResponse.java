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
    private String studentPhone;
    private String className; // 반 이름 (네이버 예약인 경우 "네이버 예약")
    private Boolean isNaverBooking; // 네이버 예약 여부
    private Long scheduleId;
    private String courseName;
    private String startTime;
    private String endTime;
    private AttendanceStatus status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalTime expectedLeaveTime;
    private LocalTime originalExpectedLeaveTime; // 원래 예정 하원 시간
    private String memo;
    private String reason;
    private Boolean classCompleted; // 수업 완료 여부
    private String teacherName; // 담당 강사명
    private String dcCheck; // D/C 체크
    private String wrCheck; // WR 체크
    
    // 추가 수업 필드들
    private Boolean vocabularyClass; // V - Vocabulary 수업
    private Boolean grammarClass; // G - Grammar 수업
    private Boolean phonicsClass; // P - Phonics 수업
    private Boolean speakingClass; // S - Speaking 수업
    private LocalTime additionalClassEndTime; // 추가 수업 종료 시간
}
