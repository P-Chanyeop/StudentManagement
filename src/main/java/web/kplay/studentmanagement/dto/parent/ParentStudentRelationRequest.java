package web.kplay.studentmanagement.dto.parent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentStudentRelationRequest {

    @NotNull(message = "보호자 ID는 필수입니다")
    private Long parentId;

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotBlank(message = "관계는 필수입니다")
    @Size(max = 20, message = "관계는 20자를 초과할 수 없습니다")
    private String relationship; // 부, 모, 조부모 등
}
