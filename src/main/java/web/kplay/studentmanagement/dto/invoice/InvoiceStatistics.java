package web.kplay.studentmanagement.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 청구서 통계 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceStatistics {

    private Long totalCount;        // 전체 청구서 수
    private Long pendingCount;      // 대기(미납) 청구서 수
    private Long paidCount;         // 납부 완료 청구서 수
    private Long overdueCount;      // 연체 청구서 수

    private Integer totalAmount;    // 전체 청구 금액
    private Integer paidAmount;     // 납부 완료 금액
    private Integer unpaidAmount;   // 미납 금액
}
