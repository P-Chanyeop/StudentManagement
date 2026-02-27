package web.kplay.studentmanagement.dto.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @JsonProperty("isNaverBooking")
    private boolean isNaverBooking;

    @JsonProperty("isManualExcel")
    private boolean isManualExcel;

    private Long naverBookingId;
    private Long attendanceId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
}
