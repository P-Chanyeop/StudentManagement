package web.kplay.studentmanagement.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAdditionalClassRequest {
    private Boolean assignedVocabulary;
    private Boolean assignedSightword;
    private Boolean assignedGrammar;
    private Boolean assignedPhonics;
}
