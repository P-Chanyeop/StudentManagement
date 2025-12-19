package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.invoice.Invoice;
import web.kplay.studentmanagement.domain.invoice.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStudentId(Long studentId);

    List<Invoice> findByStatus(InvoiceStatus status);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :date AND i.status = 'PENDING'")
    List<Invoice> findOverdueInvoices(@Param("date") LocalDate date);

    List<Invoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT i FROM Invoice i WHERE i.student.id = :studentId AND i.status = :status")
    List<Invoice> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") InvoiceStatus status);
}
