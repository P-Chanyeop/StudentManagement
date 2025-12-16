package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "수업명은 필수입니다")
    private String courseName;

    private String description;

    private Long teacherId;

    @NotNull(message = "최대 수강 인원은 필수입니다")
    @Min(value = 1, message = "최대 수강 인원은 1명 이상이어야 합니다")
    private Integer maxStudents;

    @NotNull(message = "수업 시간은 필수입니다")
    @Min(value = 10, message = "수업 시간은 최소 10분 이상이어야 합니다")
    private Integer durationMinutes;

    private String level;

    private String color;
}
