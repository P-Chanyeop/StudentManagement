package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.holiday.Holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    // 특정 날짜가 공휴일인지 확인
    Optional<Holiday> findByDate(LocalDate date);

    // 특정 기간 내의 공휴일 조회
    @Query("SELECT h FROM Holiday h WHERE h.date BETWEEN :startDate AND :endDate ORDER BY h.date")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // 특정 연도의 공휴일 조회
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date")
    List<Holiday> findByYear(@Param("year") int year);

    // 날짜 존재 여부 확인
    boolean existsByDate(LocalDate date);
}
