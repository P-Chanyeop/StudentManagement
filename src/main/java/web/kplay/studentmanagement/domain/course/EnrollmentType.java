package web.kplay.studentmanagement.domain.course;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentType {
    PERIOD("기간권", "기간제 수강권"),
    COUNT("횟수권", "횟수제 수강권");

    private final String name;
    private final String description;
}
