package web.kplay.studentmanagement.domain.attendance;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_booking_id")
    private NaverBooking naverBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private web.kplay.studentmanagement.domain.course.Course course;

    // 예약 날짜 및 시간
    @Column(nullable = false)
    private java.time.LocalDate attendanceDate;

    @Column(nullable = false)
    private LocalTime attendanceTime;

    @Column
    private Integer durationMinutes; // 수업 시간 (분)

    @Column
    private String manualStudentName; // 수동 추가 시 학생 이름

    @Column
    private String manualParentPhone; // 수동 추가 시 보호자 전화번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column
    private LocalDateTime checkInTime; // 체크인 시간

    @Column
    private LocalDateTime checkOutTime; // 체크아웃 시간

    @Column
    private LocalTime expectedLeaveTime; // 예상 하원 시간

    @Column
    private LocalTime originalExpectedLeaveTime; // 원래 예정 하원 시간 (추가 수업 전)

    @Column(length = 500)
    private String memo;

    @Column(length = 200)
    private String reason; // 결석/지각 사유

    @Column(nullable = false)
    @Builder.Default
    private Boolean classCompleted = false; // 수업 완료 여부 (체크박스)

    @Column(length = 50)
    private String dcCheck; // D/C 체크

    @Column(length = 50)
    private String wrCheck; // WR 체크

    // 추가 수업 필드들
    @Column
    @Builder.Default
    private Boolean vocabularyClass = false; // V - Vocabulary 수업

    @Column
    @Builder.Default
    private Boolean grammarClass = false; // G - Grammar 수업

    @Column
    @Builder.Default
    private Boolean phonicsClass = false; // P - Phonics 수업

    @Column
    @Builder.Default
    private Boolean speakingClass = false; // S - Speaking 수업

    @Column
    private LocalTime additionalClassEndTime; // 추가 수업 종료 시간

    @Column(nullable = false)
    @Builder.Default
    private Boolean lateNotificationSent = false; // 미출석 알림 발송 여부

    // 출석 체크
    public void checkIn(LocalDateTime checkInTime, LocalTime expectedLeaveTime) {
        this.checkInTime = checkInTime;
        LocalTime checkInLocalTime = checkInTime.toLocalTime();
        
        // 실제 체크인 시간 기준으로 하원 예정 시간 계산
        if (this.course != null && this.course.getDurationMinutes() != null) {
            this.expectedLeaveTime = checkInLocalTime.plusMinutes(this.course.getDurationMinutes());
        } else if (this.durationMinutes != null) {
            this.expectedLeaveTime = checkInLocalTime.plusMinutes(this.durationMinutes);
        } else if (expectedLeaveTime != null) {
            this.expectedLeaveTime = expectedLeaveTime;
        } else {
            this.expectedLeaveTime = checkInLocalTime.plusHours(2);
        }
        this.originalExpectedLeaveTime = this.expectedLeaveTime;
        this.status = AttendanceStatus.PRESENT;
        
        updateAdditionalClassEndTime();
    }

    // 하원 체크
    public void checkOut(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    // 출석 상태 변경
    public void updateStatus(AttendanceStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }



    // 결석 여부 확인
    public boolean isAbsent() {
        return this.status == AttendanceStatus.ABSENT;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 예상 하원 시간 업데이트
    public void updateExpectedLeaveTime(LocalTime expectedLeaveTime) {
        this.expectedLeaveTime = expectedLeaveTime;
    }

    // 원래 예정 하원 시간 업데이트
    public void updateOriginalExpectedLeaveTime(LocalTime originalExpectedLeaveTime) {
        this.originalExpectedLeaveTime = originalExpectedLeaveTime;
    }

    // 체크인 시간 직접 수정 (관리자용)
    public void updateCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    // 체크아웃 시간 직접 수정 (관리자용)
    public void updateCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    // 수업 시작 시간 수정
    public void updateAttendanceTime(LocalTime attendanceTime) {
        this.attendanceTime = attendanceTime;
    }

    // 수업 시간(분) 수정
    public void updateDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    // 수업 완료 처리
    public void completeClass() {
        this.classCompleted = true;
    }

    // 수업 완료 취소
    public void uncompleteClass() {
        this.classCompleted = false;
    }

    // 수업 완료 상태 토글
    public void toggleClassCompleted() {
        this.classCompleted = !this.classCompleted;
    }

    // D/C 체크 업데이트
    public void updateDcCheck(String dcCheck) {
        this.dcCheck = dcCheck;
    }

    // WR 체크 업데이트
    public void updateWrCheck(String wrCheck) {
        this.wrCheck = wrCheck;
    }

    // 추가 수업 토글 및 종료 시간 계산
    public void toggleVocabularyClass() {
        this.vocabularyClass = !this.vocabularyClass;
        updateAdditionalClassEndTime();
    }

    public void toggleGrammarClass() {
        this.grammarClass = !this.grammarClass;
        updateAdditionalClassEndTime();
    }

    public void togglePhonicsClass() {
        this.phonicsClass = !this.phonicsClass;
        updateAdditionalClassEndTime();
    }

    public void toggleSpeakingClass() {
        this.speakingClass = !this.speakingClass;
        updateAdditionalClassEndTime();
    }

    // 추가 수업 종료 시간 자동 계산
    private void updateAdditionalClassEndTime() {
        LocalTime baseEndTime = this.originalExpectedLeaveTime;
        if (baseEndTime == null) {
            if (this.course != null) {
                baseEndTime = this.attendanceTime.plusMinutes(this.course.getDurationMinutes());
            } else if (this.durationMinutes != null) {
                baseEndTime = this.attendanceTime.plusMinutes(this.durationMinutes);
            } else {
                baseEndTime = this.expectedLeaveTime;
            }
        }
        
        if (hasAnyAdditionalClass()) {
            // 추가 수업이 있으면 수업 종료시간 + 30분
            this.expectedLeaveTime = baseEndTime.plusMinutes(30);
            this.additionalClassEndTime = this.expectedLeaveTime;
        } else {
            // 추가 수업이 없으면 수업 종료시간 그대로
            this.expectedLeaveTime = baseEndTime;
            this.additionalClassEndTime = null;
        }
    }

    // 추가 수업이 있는지 확인
    public boolean hasAnyAdditionalClass() {
        return Boolean.TRUE.equals(vocabularyClass) || Boolean.TRUE.equals(grammarClass) 
            || Boolean.TRUE.equals(phonicsClass) || Boolean.TRUE.equals(speakingClass);
    }

    // 미출석 알림 발송 표시
    public void markLateNotificationSent() {
        this.lateNotificationSent = true;
    }

    public boolean isLateNotificationSent() {
        return this.lateNotificationSent != null && this.lateNotificationSent;
    }
}
