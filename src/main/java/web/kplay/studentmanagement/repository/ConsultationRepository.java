package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.consultation.Consultation;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByStudentId(Long studentId);

    List<Consultation> findByConsultantId(Long consultantId);

    List<Consultation> findByConsultationType(String consultationType);

    @Query("SELECT c FROM Consultation c WHERE c.consultationDate BETWEEN :startDate AND :endDate")
    List<Consultation> findByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Consultation c WHERE c.student.id = :studentId ORDER BY c.consultationDate DESC")
    List<Consultation> findByStudentIdOrderByDateDesc(@Param("studentId") Long studentId);

    // 마이페이지용 메서드
    @Query("SELECT c FROM Consultation c WHERE c.student.id = :studentId ORDER BY c.consultationDate DESC LIMIT 5")
    List<Consultation> findTop5ByStudentIdOrderByConsultationDateDesc(@Param("studentId") Long studentId);
}
