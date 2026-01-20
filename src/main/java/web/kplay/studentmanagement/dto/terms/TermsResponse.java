package web.kplay.studentmanagement.dto.terms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.kplay.studentmanagement.domain.terms.Terms;
import web.kplay.studentmanagement.domain.terms.TermsType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TermsResponse {
    private Long id;
    private TermsType type;
    private String content;
    private String version;
    private LocalDateTime effectiveDate;

    public static TermsResponse from(Terms terms) {
        return new TermsResponse(
                terms.getId(),
                terms.getType(),
                terms.getContent(),
                terms.getVersion(),
                terms.getEffectiveDate()
        );
    }
}
