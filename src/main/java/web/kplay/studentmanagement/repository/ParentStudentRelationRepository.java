package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.parent.ParentStudentRelation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentStudentRelationRepository extends JpaRepository<ParentStudentRelation, Long> {

    // 보호자 ID로 관계 조회 (활성화된 관계만)
    @Query("SELECT r FROM ParentStudentRelation r WHERE r.parent.id = :parentId AND r.isActive = true")
    List<ParentStudentRelation> findActiveRelationsByParent(@Param("parentId") Long parentId);

    // 학생 ID로 관계 조회 (활성화된 관계만)
    @Query("SELECT r FROM ParentStudentRelation r WHERE r.student.id = :studentId AND r.isActive = true")
    List<ParentStudentRelation> findActiveRelationsByStudent(@Param("studentId") Long studentId);

    // 보호자 ID와 학생 ID로 관계 조회
    @Query("SELECT r FROM ParentStudentRelation r WHERE r.parent.id = :parentId AND r.student.id = :studentId")
    Optional<ParentStudentRelation> findByParentAndStudent(
            @Param("parentId") Long parentId,
            @Param("studentId") Long studentId);

    // 보호자가 특정 학생에 대한 접근 권한이 있는지 확인
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM ParentStudentRelation r " +
            "WHERE r.parent.id = :parentId AND r.student.id = :studentId AND r.isActive = true")
    boolean hasAccessToStudent(@Param("parentId") Long parentId, @Param("studentId") Long studentId);

    // 보호자가 학생 목록 조회 (활성화된 관계)
    @Query("SELECT r.student.id FROM ParentStudentRelation r " +
            "WHERE r.parent.id = :parentId AND r.isActive = true")
    List<Long> findStudentIdsByParent(@Param("parentId") Long parentId);
}
