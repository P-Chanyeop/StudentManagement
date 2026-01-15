package web.kplay.studentmanagement.controller.reservation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/naver-booking")
public class NaverBookingSseController {

    private static final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        
        log.info("SSE 구독 추가: 현재 구독자 {}명", emitters.size());
        return emitter;
    }

    public static void notifyCrawlingComplete() {
        log.info("크롤링 완료 알림 전송: {}명에게", emitters.size());
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("crawling-complete")
                    .data("refresh"));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        });
    }
}
