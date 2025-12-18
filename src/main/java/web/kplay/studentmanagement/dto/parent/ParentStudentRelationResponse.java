package web.kplay.studentmanagement.dto.parent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentStudentRelationResponse {

    private Long id;
    private Long parentId;
    private String parentName;
    private String parentUsername;
    private Long studentId;
    private String studentName;
    private String relationship;
    private Boolean isActive;
    private Boolean canViewAttendance;
    private Boolean canViewGrades;
    private Boolean canViewInvoices;
    private Boolean canMakeReservations;
    private Boolean canReceiveMessages;
    private LocalDateTime createdAt;
}
