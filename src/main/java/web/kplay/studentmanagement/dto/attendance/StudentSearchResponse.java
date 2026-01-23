package web.kplay.studentmanagement.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSearchResponse {
    private Long studentId;
    private String studentName;
    private String parentName;
    private String parentPhone;
    private String school;
    private String courseName;
    private boolean isNaverBooking;
    private Long naverBookingId;
}
