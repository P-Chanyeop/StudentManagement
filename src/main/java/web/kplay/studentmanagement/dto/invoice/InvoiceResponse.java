package web.kplay.studentmanagement.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.invoice.InvoiceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 청구서 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private String parentName;
    private String parentPhone;
    private Long issuedById;
    private String issuedByName;
    private String title;
    private Integer amount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String description;
    private String invoiceNumber;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 계산 필드
    private Integer daysUntilDue; // 납부 기한까지 남은 일수
    private Boolean isOverdue; // 연체 여부
}
