package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {
    private Long id;
    private String courseName;
    private String description;
    private Long teacherId;
    private String teacherName;
    private Integer maxStudents;
    private Integer durationMinutes;
    private String level;
    private String color;
    private Boolean isActive;
}
