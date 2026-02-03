package web.kplay.studentmanagement.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenaissanceIdUploadDto {
    private String studentName;
    private String renaissanceUsername;
    private String parentPhone;
}
