package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import web.kplay.studentmanagement.domain.reservation.BlockedTimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BlockedTimeSlotRepository extends JpaRepository<BlockedTimeSlot, Long> {

    @Query("SELECT b FROM BlockedTimeSlot b WHERE b.isActive = true AND (" +
           "(b.blockType = 'SINGLE' AND b.blockDate = :date) OR " +
           "(b.blockType = 'RANGE' AND b.startDate <= :date AND b.endDate >= :date) OR " +
           "(b.blockType = 'WEEKLY' AND b.dayOfWeek = :dayOfWeek))")
    List<BlockedTimeSlot> findActiveBlocksForDate(@Param("date") LocalDate date, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    List<BlockedTimeSlot> findByIsActiveTrue();
}
