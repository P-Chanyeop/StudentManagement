package web.kplay.studentmanagement.dto.makeup;

import jakarta.validation.constraints.NotNull;
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
public class MakeupClassCreateRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "수업 ID는 필수입니다")
    private Long courseId;

    @NotNull(message = "원래 수업 날짜는 필수입니다")
    private LocalDate originalDate;

    @NotNull(message = "보강 수업 날짜는 필수입니다")
    private LocalDate makeupDate;

    @NotNull(message = "보강 수업 시간은 필수입니다")
    private LocalTime makeupTime;

    private String reason;

    private String memo;
}
