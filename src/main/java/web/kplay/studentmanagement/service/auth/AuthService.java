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
import web.kplay.studentmanagement.dto.auth.RegisterRequest;
import web.kplay.studentmanagement.dto.auth.SignupRequest;

import java.util.Map;
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
        // 필수 약관 동의 체크
        if (!Boolean.TRUE.equals(signupRequest.getTermsAgreed()) || 
            !Boolean.TRUE.equals(signupRequest.getPrivacyAgreed())) {
            throw new BusinessException("필수 약관에 동의해주세요");
        }

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
                .termsAgreed(signupRequest.getTermsAgreed())
                .privacyAgreed(signupRequest.getPrivacyAgreed())
                .marketingAgreed(Optional.ofNullable(signupRequest.getMarketingAgreed()).orElse(false))
                .smsAgreed(Optional.ofNullable(signupRequest.getSmsAgreed()).orElse(false))
                .agreedAt(java.time.LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
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
        log.info("User logged out: {}", username);
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
                .address(user.getAddress())
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

    @Transactional
    public UserProfileResponse updateProfile(Long userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        if (updates.containsKey("name")) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber(updates.get("phoneNumber"));
        }
        if (updates.containsKey("address")) {
            user.setAddress(updates.get("address"));
        }

        userRepository.save(user);
        return getUserProfile(userId);
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

    /**
     * 학부모 회원가입 (학생 정보 포함)
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 중복 사용자명 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 비밀번호 검증 강화
        validatePassword(request.getPassword());

        // 학부모 계정 생성
        User parentUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.PARENT)
                .isActive(true)
                .build();

        User savedParent = userRepository.save(parentUser);

        // 학생 정보가 있으면 학생도 생성
        if (request.getStudent() != null) {
            RegisterRequest.StudentInfo studentInfo = request.getStudent();
            
            Student student = Student.builder()
                    .parentUser(savedParent)
                    .studentName(studentInfo.getStudentName())
                    .studentPhone(studentInfo.getStudentPhone())
                    .birthDate(LocalDate.parse(studentInfo.getBirthDate()))
                    .gender(studentInfo.getGender())
                    .school(studentInfo.getSchool())
                    .grade(studentInfo.getGrade())
                    .englishLevel(studentInfo.getEnglishLevel())
                    .parentName(request.getName())
                    .parentPhone(request.getPhoneNumber())
                    .address(request.getAddress())
                    .isActive(true)
                    .build();

            studentRepository.save(student);
        }

        log.info("학부모 회원가입 완료 - 사용자명: {}, 이름: {}", request.getUsername(), request.getName());
    }

    /**
     * 아이디 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 비밀번호 유효성 검증
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        
        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8글자 이상이어야 합니다.");
        }
        
        // 영어 포함 여부 확인
        if (!password.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException("비밀번호에 영어가 포함되어야 합니다.");
        }
        
        // 숫자 포함 여부 확인
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("비밀번호에 숫자가 포함되어야 합니다.");
        }
        
        // 전체 패턴 검증
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 영어와 숫자를 포함하여 8글자 이상이어야 합니다.");
        }
    }
}
