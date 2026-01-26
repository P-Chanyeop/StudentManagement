package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import web.kplay.studentmanagement.domain.sms.SMSTemplate;
import web.kplay.studentmanagement.repository.SMSTemplateRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SMSTemplateInitializer implements CommandLineRunner {

    private final SMSTemplateRepository smsTemplateRepository;

    @Override
    public void run(String... args) {
        if (smsTemplateRepository.count() == 0) {
            initializeTemplates();
        }
    }

    private void initializeTemplates() {
        // 단어책/교재 안내
        smsTemplateRepository.save(SMSTemplate.builder()
                .name("단어책/교재 안내")
                .category("textbook")
                .content("안녕하세요.\n리틀베어 리딩클럽입니다.\n\n{studentName} 학생 필요한 교재 안내드립니다.\n\n{textbookName} 교재\n다음 등원할 때 아이 편으로 보내주세요\n\n감사합니다! :)")
                .description("교재 구매 안내 시 사용")
                .isActive(true)
                .build());

        // 일반 안내
        smsTemplateRepository.save(SMSTemplate.builder()
                .name("일반 안내")
                .category("general")
                .content("안녕하세요.\n리틀베어 리딩클럽입니다.\n\n{content}\n\n감사합니다! :)")
                .description("일반 안내 문자")
                .isActive(true)
                .build());

        // 휴원 안내
        smsTemplateRepository.save(SMSTemplate.builder()
                .name("휴원 안내")
                .category("notice")
                .content("안녕하세요.\n리틀베어 리딩클럽입니다.\n\n{date} 휴원 안내드립니다.\n\n{reason}\n\n감사합니다! :)")
                .description("휴원일 안내")
                .isActive(true)
                .build());

        // 수강 횟수 소진 안내
        smsTemplateRepository.save(SMSTemplate.builder()
                .name("수강 횟수 소진 안내")
                .category("enrollment")
                .content("안녕하세요.\n리틀베어 리딩클럽입니다.\n\n{studentName} 학생의 횟수가 모두 소진되었습니다.\n\n재등록을 원하신다면 결제 부탁드립니다.\n감사합니다! :)")
                .description("수강 횟수 소진 시 자동 발송")
                .isActive(true)
                .build());

        log.info("SMS 템플릿 초기화 완료");
    }
}
