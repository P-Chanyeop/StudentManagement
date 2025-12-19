package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강권 수동 조절 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentAdjustRequest {

    @NotNull(message = "조절할 횟수는 필수입니다")
    private Integer adjustment; // 양수: 증가, 음수: 감소

    @NotNull(message = "조절 사유는 필수입니다")
    private String reason; // 조절 사유 (예: "결석으로 인한 차감", "보강으로 인한 복구")
}
