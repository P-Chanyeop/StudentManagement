package web.kplay.studentmanagement.dto.invoice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 청구서 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceUpdateRequest {

    @NotBlank(message = "청구서 제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
    private String title;

    @NotNull(message = "청구 금액은 필수입니다")
    @Min(value = 0, message = "청구 금액은 0원 이상이어야 합니다")
    @Max(value = 100000000, message = "청구 금액은 1억원을 초과할 수 없습니다")
    private Integer amount;

    @NotNull(message = "납부 기한은 필수입니다")
    private LocalDate dueDate;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;
}
