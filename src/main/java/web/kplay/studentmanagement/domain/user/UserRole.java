package web.kplay.studentmanagement.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN("ROLE_ADMIN", "관리자"),
    TEACHER("ROLE_TEACHER", "선생님"),
    PARENT("ROLE_PARENT", "학부모");

    private final String key;
    private final String description;
}
