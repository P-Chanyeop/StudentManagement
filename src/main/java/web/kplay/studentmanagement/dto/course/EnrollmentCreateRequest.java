package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.course.EnrollmentType;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentCreateRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "수업 ID는 필수입니다")
    private Long courseId;

    @NotNull(message = "수강권 타입은 필수입니다")
    private EnrollmentType enrollmentType;

    // 기간권용
    private LocalDate startDate;
    private LocalDate endDate;

    // 횟수권용
    @Min(value = 1, message = "총 횟수는 1회 이상이어야 합니다")
    private Integer totalCount;

    private String memo;
}
