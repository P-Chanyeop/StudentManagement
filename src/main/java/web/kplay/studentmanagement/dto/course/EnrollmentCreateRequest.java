package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentCreateRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    // 수업 ID는 선택사항 (반 등록은 학생 정보에서 별도 관리)
    private Long courseId;

    // 모든 수강권은 기간 + 횟수 모두 필수
    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    // 종료일 (선택사항 - 없으면 시작일 + 총횟수로 공휴일 제외하여 자동 계산)
    private LocalDate endDate;

    @NotNull(message = "총 횟수는 필수입니다")
    @Min(value = 1, message = "총 횟수는 1회 이상이어야 합니다")
    private Integer totalCount;

    private Integer remainingCount;

    private String memo;
}
