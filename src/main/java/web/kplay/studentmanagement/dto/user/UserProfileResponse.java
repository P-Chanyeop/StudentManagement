package web.kplay.studentmanagement.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long userId;
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;

    // 학생인 경우
    private Long studentId;
    private String studentName;

    // 수강권 요약 정보
    private List<EnrollmentSummary> enrollmentSummaries;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentSummary {
        private Long enrollmentId;
        private String courseName;
        private String enrollmentType; // PERIOD or COUNT
        private LocalDate endDate; // 기간권인 경우
        private Integer remainingCount; // 횟수권인 경우
        private Integer totalCount; // 횟수권인 경우
        private Long daysRemaining; // 기간권 남은 일수
        private Boolean isExpiring; // 만료 임박 여부 (7일 이내)
    }
}
