package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;

import java.util.List;
import java.util.Optional;

public interface NaverBookingRepository extends JpaRepository<NaverBooking, Long> {
    Optional<NaverBooking> findByBookingNumber(String bookingNumber);
    
    @Query("SELECT nb FROM NaverBooking nb WHERE nb.bookingTime LIKE CONCAT(:date, '%')")
    List<NaverBooking> findByBookingDate(@Param("date") String date);
}
