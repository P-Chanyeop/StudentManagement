package web.kplay.studentmanagement.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.dto.auth.JwtResponse;
import web.kplay.studentmanagement.dto.auth.LoginRequest;
import web.kplay.studentmanagement.dto.auth.RefreshTokenRequest;
import web.kplay.studentmanagement.dto.auth.SignupRequest;
import web.kplay.studentmanagement.dto.user.UserProfileResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.security.JwtTokenProvider;
import web.kplay.studentmanagement.security.UserDetailsImpl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getUsername());

        // Refresh Token 저장
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new JwtResponse(
                accessToken,
                refreshToken,
                userDetails.getId(),
                userDetails.getUsername(),
                user.getName(),
                userDetails.getRole()
        );
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new BusinessException("이미 존재하는 사용자명입니다");
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .name(signupRequest.getName())
                .phoneNumber(signupRequest.getPhoneNumber())
                .email(signupRequest.getEmail())
                .role(signupRequest.getRole())
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("새 사용자 등록 완료: {}", user.getUsername());
    }

    @Transactional
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("유효하지 않은 리프레시 토큰입니다");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException("리프레시 토큰을 찾을 수 없습니다"));

        if (!user.getUsername().equals(username)) {
            throw new BusinessException("토큰 정보가 일치하지 않습니다");
        }

        // 새로운 Access Token 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(user),
                null,
                UserDetailsImpl.build(user).getAuthorities()
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        return new JwtResponse(
                newAccessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole()
        );
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        user.updateRefreshToken(null);
        userRepository.save(user);
        log.info("사용자 로그아웃: {}", username);
    }

    /**
     * 사용자 프로필 조회 (헤더 표시용)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name());

        // 부모인 경우 자녀 정보 조회
        if (user.getRole() == UserRole.PARENT) {
            List<Student> children = studentRepository.findByParentUser(user);
            if (!children.isEmpty()) {
                // 첫 번째 자녀 정보를 기본으로 설정
                Student firstChild = children.get(0);
                builder.studentId(firstChild.getId())
                        .studentName(firstChild.getStudentName());

                // 활성 수강권 목록 조회
                List<Enrollment> activeEnrollments = enrollmentRepository
                        .findByStudentIdAndIsActiveTrue(firstChild.getId());

                List<UserProfileResponse.EnrollmentSummary> summaries = activeEnrollments.stream()
                        .map(this::toEnrollmentSummary)
                        .collect(Collectors.toList());

                builder.enrollmentSummaries(summaries);
            } else {
                builder.enrollmentSummaries(List.of());
            }
        } else {
            builder.enrollmentSummaries(List.of());
        }

        return builder.build();
    }

    /**
     * Enrollment를 EnrollmentSummary로 변환
     * 모든 수강권은 기간 + 횟수를 모두 가짐
     */
    private UserProfileResponse.EnrollmentSummary toEnrollmentSummary(Enrollment enrollment) {
        // 기간 정보 계산
        LocalDate endDate = enrollment.getEndDate();
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), endDate);

        // 만료 임박 여부: 기간 7일 이하 또는 횟수 3회 이하
        boolean isExpiring = (daysRemaining <= 7 && daysRemaining >= 0) ||
                            (enrollment.getRemainingCount() != null && enrollment.getRemainingCount() <= 3);

        return UserProfileResponse.EnrollmentSummary.builder()
                .enrollmentId(enrollment.getId())
                .courseName(enrollment.getCourse().getCourseName())
                .endDate(endDate)
                .daysRemaining(daysRemaining)
                .remainingCount(enrollment.getRemainingCount())
                .totalCount(enrollment.getTotalCount())
                .isExpiring(isExpiring)
                .build();
    }
}
