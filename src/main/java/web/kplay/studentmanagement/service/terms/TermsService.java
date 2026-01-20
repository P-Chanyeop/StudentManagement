package web.kplay.studentmanagement.service.terms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.terms.Terms;
import web.kplay.studentmanagement.domain.terms.TermsType;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.TermsRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private final TermsRepository termsRepository;

    public Terms getActiveTerms(TermsType type) {
        return termsRepository.findByTypeAndIsActiveTrue(type)
                .orElseThrow(() -> new ResourceNotFoundException("활성화된 약관을 찾을 수 없습니다: " + type));
    }
}
