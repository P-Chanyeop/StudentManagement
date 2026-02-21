package web.kplay.studentmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentHoldRequest {
    private LocalDate holdStartDate;
    private LocalDate holdEndDate;
    private LocalDate newEndDate; // 프론트에서 공휴일 반영하여 계산한 새 종료일
}
