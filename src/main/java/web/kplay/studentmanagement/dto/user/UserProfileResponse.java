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
        // 모든 수강권은 기간 + 횟수를 모두 가짐
        private LocalDate endDate; // 종료일
        private Integer remainingCount; // 남은 횟수
        private Integer totalCount; // 전체 횟수
        private Long daysRemaining; // 남은 일수
        private Boolean isExpiring; // 만료 임박 여부 (기간 7일 이내 or 횟수 3회 이하)
    }
}
