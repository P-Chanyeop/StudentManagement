package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourseScheduleCreateRequest {

    @NotNull(message = "수업 ID는 필수입니다")
    private Long courseId;

    @NotNull(message = "수업 날짜는 필수입니다")
    private LocalDate scheduleDate;

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalTime endTime;

    private String dayOfWeek;

    private String memo;
}
