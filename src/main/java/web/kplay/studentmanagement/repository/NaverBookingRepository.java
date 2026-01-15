package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;

import java.util.Optional;

public interface NaverBookingRepository extends JpaRepository<NaverBooking, Long> {
    Optional<NaverBooking> findByBookingNumber(String bookingNumber);
}
