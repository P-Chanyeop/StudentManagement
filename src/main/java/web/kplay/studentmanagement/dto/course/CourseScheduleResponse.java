package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseScheduleResponse {
    private Long id;
    private Long courseId;
    private String courseName;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dayOfWeek;
    private Integer currentStudents;
    private Integer maxStudents;
    private Boolean isCancelled;
    private String cancelReason;
    private String memo;
}
