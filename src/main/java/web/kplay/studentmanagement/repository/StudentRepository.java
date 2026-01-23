package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.student.Student;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByIsActive(Boolean isActive);

    @Query("SELECT s FROM Student s WHERE s.studentName LIKE %:keyword% OR s.parentName LIKE %:keyword%")
    List<Student> searchByKeyword(@Param("keyword") String keyword);

    List<Student> findByEnglishLevel(String englishLevel);

    @Query("SELECT s FROM Student s WHERE s.parentPhone = :phone")
    List<Student> findByParentPhone(@Param("phone") String phone);
    
    List<Student> findByParentPhoneAndIsActive(String parentPhone, Boolean isActive);
    
    List<Student> findByParentUser(web.kplay.studentmanagement.domain.user.User parentUser);
    
    List<Student> findByStudentName(String studentName);
}
