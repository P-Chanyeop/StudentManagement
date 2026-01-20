package web.kplay.studentmanagement.controller.terms;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.terms.TermsType;
import web.kplay.studentmanagement.dto.terms.TermsResponse;
import web.kplay.studentmanagement.service.terms.TermsService;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/{type}")
    public ResponseEntity<TermsResponse> getTerms(@PathVariable TermsType type) {
        return ResponseEntity.ok(
                TermsResponse.from(termsService.getActiveTerms(type))
        );
    }
}
