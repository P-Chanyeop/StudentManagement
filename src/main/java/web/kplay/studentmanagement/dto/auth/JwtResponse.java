package web.kplay.studentmanagement.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.user.UserRole;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String name;
    private String nickname;
    private UserRole role;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String name, String nickname, UserRole role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.name = name;
        this.nickname = nickname;
        this.role = role;
    }
}
