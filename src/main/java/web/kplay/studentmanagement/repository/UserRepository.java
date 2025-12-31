package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(UserRole role);

    List<User> findByIsActive(Boolean isActive);

    Optional<User> findByRefreshToken(String refreshToken);

    /**
     * 특정 역할과 활성 상태의 사용자 수 조회
     * @param role 사용자 역할
     * @param isActive 활성 상태
     * @return 해당 조건의 사용자 수
     */
    int countByRoleAndIsActive(UserRole role, Boolean isActive);
}
