package web.kplay.studentmanagement.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.dto.consultation.ConsultationResponse;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
import web.kplay.studentmanagement.dto.leveltest.LevelTestResponse;
import web.kplay.studentmanagement.dto.message.MessageResponse;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.dto.student.StudentResponse;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponse {

    // 학생 기본 정보
    private StudentResponse studentInfo;

    // 수강권 정보 (활성화된 수강권만)
    private List<EnrollmentResponse> activeEnrollments;

    // 최근 출석 기록 (최근 10개)
    private List<AttendanceResponse> recentAttendances;

    // 예약 내역 (예정된 예약만)
    private List<ReservationResponse> upcomingReservations;

    // 레벨테스트 일정 (예정된 테스트만)
    private List<LevelTestResponse> upcomingLevelTests;

    // 최근 받은 문자 메시지 (최근 20개)
    private List<MessageResponse> recentMessages;

    // 상담 기록 (최근 5개)
    private List<ConsultationResponse> recentConsultations;

    // 통계 정보
    private MyPageStats stats;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyPageStats {
        // 총 출석 횟수
        private Long totalAttendanceCount;

        // 이번 달 출석 횟수
        private Long monthlyAttendanceCount;

        // 총 지각 횟수
        private Long totalLateCount;

        // 총 결석 횟수
        private Long totalAbsentCount;

        // 활성 수강권 개수
        private Integer activeEnrollmentCount;

        // 예정된 예약 개수
        private Integer upcomingReservationCount;

        // 상담 개수 (선생님용)
        private Integer consultationCount;

        // 출석률 (계산된 값)
        private Double attendanceRate;
    }
}
