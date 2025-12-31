package web.kplay.studentmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.DashboardStatsResponse;
import web.kplay.studentmanagement.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(Authentication authentication) {
        log.info("Dashboard stats API called by user: {}", authentication.getName());
        DashboardStatsResponse stats = dashboardService.getDashboardStats(authentication.getName());
        log.info("Dashboard stats response: {}", stats);
        return ResponseEntity.ok(stats);
    }
}
