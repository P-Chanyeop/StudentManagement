package web.kplay.studentmanagement.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserMenuSetting;
import web.kplay.studentmanagement.dto.user.MenuOrderRequest;
import web.kplay.studentmanagement.dto.user.MenuOrderResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.UserMenuSettingRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMenuSettingService {

    private final UserMenuSettingRepository userMenuSettingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveMenuOrder(Long userId, MenuOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        try {
            String menuOrderJson = objectMapper.writeValueAsString(request.getMenuPaths());
            
            UserMenuSetting setting = userMenuSettingRepository.findByUserId(userId)
                    .orElse(UserMenuSetting.builder()
                            .user(user)
                            .menuOrder(menuOrderJson)
                            .build());
            
            setting.updateMenuOrder(menuOrderJson);
            userMenuSettingRepository.save(setting);
            
        } catch (JsonProcessingException e) {
            log.error("메뉴 순서 JSON 변환 실패", e);
            throw new RuntimeException("메뉴 순서 저장에 실패했습니다");
        }
    }

    @Transactional(readOnly = true)
    public MenuOrderResponse getMenuOrder(Long userId) {
        return userMenuSettingRepository.findByUserId(userId)
                .map(setting -> {
                    try {
                        List<String> menuPaths = objectMapper.readValue(
                                setting.getMenuOrder(), 
                                new TypeReference<List<String>>() {}
                        );
                        return MenuOrderResponse.builder()
                                .menuPaths(menuPaths)
                                .build();
                    } catch (JsonProcessingException e) {
                        log.error("메뉴 순서 JSON 파싱 실패", e);
                        return MenuOrderResponse.builder().build();
                    }
                })
                .orElse(MenuOrderResponse.builder().build());
    }
}
