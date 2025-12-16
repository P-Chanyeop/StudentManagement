package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.course.EnrollmentType;

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
    private EnrollmentType enrollmentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalCount;
    private Integer usedCount;
    private Integer remainingCount;
    private Boolean isActive;
    private String memo;
}
