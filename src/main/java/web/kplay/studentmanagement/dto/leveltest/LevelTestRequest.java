package web.kplay.studentmanagement.dto.leveltest;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LevelTestRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    private Long teacherId;

    @NotNull(message = "테스트 날짜는 필수입니다")
    private LocalDate testDate;

    @NotNull(message = "테스트 시간은 필수입니다")
    private LocalTime testTime;

    private String memo;
}
