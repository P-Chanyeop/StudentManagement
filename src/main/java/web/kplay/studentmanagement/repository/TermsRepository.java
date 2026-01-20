package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.terms.Terms;
import web.kplay.studentmanagement.domain.terms.TermsType;

import java.util.Optional;

@Repository
public interface TermsRepository extends JpaRepository<Terms, Long> {
    Optional<Terms> findByTypeAndIsActiveTrue(TermsType type);
}
