package web.kplay.studentmanagement.domain.user;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column
    private String refreshToken;

    // 약관 동의 필드
    @Column(nullable = false)
    @Builder.Default
    private Boolean termsAgreed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean privacyAgreed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean marketingAgreed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsAgreed = false;

    @Column
    private LocalDateTime agreedAt;

    // 비밀번호 변경
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 리프레시 토큰 업데이트
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // 사용자 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 사용자 활성화
    public void activate() {
        this.isActive = true;
    }

    // 정보 업데이트
    public void updateInfo(String name, String phoneNumber, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
