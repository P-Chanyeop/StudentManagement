package web.kplay.studentmanagement.controller.mypage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.mypage.MyPageResponse;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.mypage.MyPageService;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 내 마이페이지 조회 (로그인한 사용자)
     * STUDENT, PARENT 역할 모두 접근 가능
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ResponseEntity<MyPageResponse> getMyPage(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("마이페이지 조회 요청: userId={}", userId);
        MyPageResponse response = myPageService.getMyPageByUserId(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 학생의 마이페이지 조회
     * ADMIN, TEACHER, PARENT 역할 접근 가능
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<MyPageResponse> getStudentMyPage(@PathVariable Long studentId) {
        log.info("학생 마이페이지 조회 요청: studentId={}", studentId);
        MyPageResponse response = myPageService.getMyPageByStudentId(studentId);

        return ResponseEntity.ok(response);
    }
}
