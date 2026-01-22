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
}
