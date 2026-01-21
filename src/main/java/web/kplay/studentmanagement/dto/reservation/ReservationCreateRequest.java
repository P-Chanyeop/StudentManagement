package web.kplay.studentmanagement.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCreateRequest {

    // 기존 학생 ID (기존 학생용)
    private Long studentId;

    @NotNull(message = "예약 날짜는 필수입니다")
    private LocalDate reservationDate;

    @NotNull(message = "예약 시간은 필수입니다")
    private LocalTime reservationTime;

    private Long enrollmentId;

    private String memo;

    private String consultationType; // 상담 유형

    private String reservationSource; // WEB, NAVER, PHONE 등

    // 신규 예약용 학부모/학생 정보
    private String parentName;
    private String parentPhone;
    private List<StudentInfo> students;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private String studentName;
        private String studentPhone;
        private String school;
    }
}
