package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.kplay.studentmanagement.domain.reservation.Waitlist;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByWaitDateAndWaitTimeAndConsultationTypeAndActiveTrue(LocalDate date, LocalTime time, String type);
    List<Waitlist> findByStudentIdAndActiveTrue(Long studentId);
    boolean existsByStudentIdAndWaitDateAndWaitTimeAndConsultationTypeAndActiveTrue(Long studentId, LocalDate date, LocalTime time, String type);
}
