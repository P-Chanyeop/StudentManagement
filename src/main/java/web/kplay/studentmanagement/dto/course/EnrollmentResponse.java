package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.dto.student.StudentResponse;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private StudentResponse student; // 학생 객체 추가
    private Long courseId;
    private String courseName;
    private CourseResponse course; // 수업 객체 추가
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
    private String recordingStatus; // 레코딩 파일 현황 (예: "0/2", "1/4")
    private Integer expectedRecordings; // 예상 레코딩 파일 수
    private Integer actualRecordings; // 실제 레코딩 파일 수
    
    // 홀딩 관련 필드
    private LocalDate holdStartDate;
    private LocalDate holdEndDate;
    private Boolean isOnHold;
    private Integer totalHoldDays;
}
