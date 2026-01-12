package web.kplay.studentmanagement.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.user.MenuOrderRequest;
import web.kplay.studentmanagement.dto.user.MenuOrderResponse;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.user.UserMenuSettingService;

@RestController
@RequestMapping("/api/user/menu-settings")
@RequiredArgsConstructor
public class UserMenuSettingController {

    private final UserMenuSettingService userMenuSettingService;

    @PostMapping
    public ResponseEntity<Void> saveMenuOrder(
            @RequestBody MenuOrderRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        userMenuSettingService.saveMenuOrder(userDetails.getId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<MenuOrderResponse> getMenuOrder(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        MenuOrderResponse response = userMenuSettingService.getMenuOrder(userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
