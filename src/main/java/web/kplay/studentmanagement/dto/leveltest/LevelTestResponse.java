package web.kplay.studentmanagement.dto.leveltest;

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
public class LevelTestResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long teacherId;
    private String teacherName;
    private LocalDate testDate;
    private LocalTime testTime;
    private String testStatus;
    private String testResult;
    private Integer testScore;
    private String feedback;
    private String strengths;
    private String improvements;
    private String recommendedLevel;
    private String memo;
    private Boolean messageNotificationSent;
}
