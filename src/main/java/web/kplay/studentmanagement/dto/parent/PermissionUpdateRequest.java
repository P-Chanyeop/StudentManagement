package web.kplay.studentmanagement.dto.parent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionUpdateRequest {

    private Boolean canViewAttendance;
    private Boolean canViewGrades;
    private Boolean canViewInvoices;
    private Boolean canMakeReservations;
    private Boolean canReceiveMessages;
}
