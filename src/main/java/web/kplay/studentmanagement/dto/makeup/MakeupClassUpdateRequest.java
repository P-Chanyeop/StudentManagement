package web.kplay.studentmanagement.dto.makeup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.makeup.MakeupStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeupClassUpdateRequest {

    private LocalDate makeupDate;
    private LocalTime makeupTime;
    private String reason;
    private MakeupStatus status;
    private String memo;
}
