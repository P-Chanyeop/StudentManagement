package web.kplay.studentmanagement.domain.attendance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("출석", "정상 출석"),
    LATE("지각", "지각"),
    ABSENT("결석", "결석"),
    EXCUSED("사유결석", "사유가 있는 결석"),
    EARLY_LEAVE("조퇴", "조퇴");

    private final String name;
    private final String description;
}
