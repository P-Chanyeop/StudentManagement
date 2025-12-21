package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    // 모든 수강권은 기간 + 횟수를 모두 가짐
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalCount;
    private Integer usedCount;
    private Integer remainingCount;
    private Boolean isActive;
    private Integer customDurationMinutes; // 개별 수업 시간
    private Integer actualDurationMinutes; // 실제 적용되는 수업 시간
    private String memo;
}
