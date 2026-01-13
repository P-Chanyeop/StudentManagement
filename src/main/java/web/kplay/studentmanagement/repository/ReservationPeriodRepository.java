package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.kplay.studentmanagement.domain.reservation.ReservationPeriod;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReservationPeriodRepository extends JpaRepository<ReservationPeriod, Long> {
    
    @Query("SELECT rp FROM ReservationPeriod rp WHERE rp.isActive = true AND :now BETWEEN rp.openTime AND rp.closeTime")
    Optional<ReservationPeriod> findActiveReservationPeriod(LocalDateTime now);
    
    @Query("SELECT rp FROM ReservationPeriod rp WHERE rp.isActive = true ORDER BY rp.openTime DESC")
    Optional<ReservationPeriod> findLatestActivePeriod();
}
