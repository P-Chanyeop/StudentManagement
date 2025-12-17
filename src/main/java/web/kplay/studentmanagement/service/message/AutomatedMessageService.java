package web.kplay.studentmanagement.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.leveltest.LevelTest;
import web.kplay.studentmanagement.domain.message.Message;
import web.kplay.studentmanagement.domain.message.MessageType;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.LevelTestRepository;
import web.kplay.studentmanagement.repository.MessageRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 자동화된 문자 메시지 발송 서비스
 * - 지각 자동 알림 (10분 지각 시)
 * - 수강권 만료 예정 알림 (4주 이내, 첫 수업 시작 시)
 * - 레벨테스트 전날 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatedMessageService {

    private final MessageRepository messageRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LevelTestRepository levelTestRepository;

    /**
     * 지각 자동 알림 발송
     * 출석 체크인 시 지각(10분 이상) 판정된 경우 자동 호출
     */
    @Transactional
    public void sendLateNotification(Student student, LocalDateTime checkInTime, LocalDateTime scheduledTime) {
        long minutesLate = ChronoUnit.MINUTES.between(scheduledTime, checkInTime);

        String content = String.format(
                "[K-PLAY 학원] 안녕하세요. %s 학생이 %d분 지각하여 %s에 등원하였습니다. 참고 부탁드립니다.",
                student.getStudentName(),
                minutesLate,
                checkInTime.toLocalTime()
        );

        Message message = Message.builder()
                .student(student)
                .recipientPhone(student.getParentPhone())
                .recipientName(student.getParentName())
                .messageType(MessageType.LATE_NOTIFICATION)
                .content(content)
                .sendStatus("PENDING")
                .build();

        Message savedMessage = messageRepository.save(message);

        // 실제 SMS API 연동 시 여기서 발송 처리
        // 현재는 테스트용으로 자동 발송 완료 처리
        savedMessage.markAsSent(LocalDateTime.now(), "AUTO-LATE-" + savedMessage.getId());

        log.info("지각 자동 알림 발송: 학생={}, 지각시간={}분", student.getStudentName(), minutesLate);
    }

    /**
     * 수강권 만료 예정 알림 (매일 오전 9시 실행)
     * - 4주 이내 만료 예정 수강권
     * - 첫 수업 시작한 학생 대상
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    @Transactional
    public void sendEnrollmentExpiryNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate fourWeeksLater = today.plusWeeks(4);

        // 4주 이내 만료 예정 기간권 조회
        List<Enrollment> expiringEnrollments = enrollmentRepository.findExpiringEnrollments(today, fourWeeksLater);

        for (Enrollment enrollment : expiringEnrollments) {
            // 이미 오늘 알림을 보낸 경우 스킵
            if (hasRecentNotification(enrollment.getStudent(), MessageType.ENROLLMENT_EXPIRY, 1)) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, enrollment.getEndDate());
            String content = String.format(
                    "[K-PLAY 학원] 안녕하세요. %s 학생의 %s 수강권이 %d일 후 만료 예정입니다. 갱신을 원하시면 연락 부탁드립니다.",
                    enrollment.getStudent().getStudentName(),
                    enrollment.getCourse().getCourseName(),
                    daysRemaining
            );

            Message message = Message.builder()
                    .student(enrollment.getStudent())
                    .recipientPhone(enrollment.getStudent().getParentPhone())
                    .recipientName(enrollment.getStudent().getParentName())
                    .messageType(MessageType.ENROLLMENT_EXPIRY)
                    .content(content)
                    .sendStatus("PENDING")
                    .build();

            Message savedMessage = messageRepository.save(message);
            savedMessage.markAsSent(LocalDateTime.now(), "AUTO-EXPIRY-" + savedMessage.getId());

            log.info("수강권 만료 알림 발송: 학생={}, 만료일={}, 남은일수={}",
                    enrollment.getStudent().getStudentName(), enrollment.getEndDate(), daysRemaining);
        }

        log.info("수강권 만료 알림 일괄 발송 완료: 총 {}건", expiringEnrollments.size());
    }

    /**
     * 만료된 수강권 재등록 안내 (매주 월요일 오전 9시)
     */
    @Scheduled(cron = "0 0 9 * * MON") // 매주 월요일 오전 9시
    @Transactional
    public void sendExpiredEnrollmentReminders() {
        List<Enrollment> expiredEnrollments = enrollmentRepository.findExpiredEnrollments();

        for (Enrollment enrollment : expiredEnrollments) {
            // 최근 1주일 이내 알림을 보낸 경우 스킵
            if (hasRecentNotification(enrollment.getStudent(), MessageType.ENROLLMENT_EXPIRY, 7)) {
                continue;
            }

            String content = String.format(
                    "[K-PLAY 학원] 안녕하세요. %s 학생의 %s 수강권이 만료되었습니다. 재등록을 원하시면 연락 부탁드립니다.",
                    enrollment.getStudent().getStudentName(),
                    enrollment.getCourse().getCourseName()
            );

            Message message = Message.builder()
                    .student(enrollment.getStudent())
                    .recipientPhone(enrollment.getStudent().getParentPhone())
                    .recipientName(enrollment.getStudent().getParentName())
                    .messageType(MessageType.ENROLLMENT_EXPIRY)
                    .content(content)
                    .sendStatus("PENDING")
                    .build();

            Message savedMessage = messageRepository.save(message);
            savedMessage.markAsSent(LocalDateTime.now(), "AUTO-EXPIRED-" + savedMessage.getId());

            log.info("만료 수강권 재등록 안내 발송: 학생={}", enrollment.getStudent().getStudentName());
        }

        log.info("만료 수강권 재등록 안내 발송 완료: 총 {}건", expiredEnrollments.size());
    }

    /**
     * 레벨테스트 전날 알림 (매일 오후 5시 실행)
     */
    @Scheduled(cron = "0 0 17 * * *") // 매일 오후 5시
    @Transactional
    public void sendLevelTestReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // 내일 예정된 레벨테스트 조회
        List<LevelTest> tomorrowTests = levelTestRepository.findByTestDate(tomorrow);

        for (LevelTest test : tomorrowTests) {
            // 이미 취소된 테스트는 스킵
            if ("CANCELLED".equals(test.getTestStatus())) {
                continue;
            }

            // 이미 알림을 보낸 경우 스킵
            if (test.getMessageNotificationSent()) {
                continue;
            }

            String content = String.format(
                    "[K-PLAY 학원] 안녕하세요. %s 학생의 레벨테스트가 내일(%s) %s에 예정되어 있습니다. 시간 맞춰 방문 부탁드립니다.",
                    test.getStudent().getStudentName(),
                    test.getTestDate(),
                    test.getTestTime()
            );

            Message message = Message.builder()
                    .student(test.getStudent())
                    .recipientPhone(test.getStudent().getParentPhone())
                    .recipientName(test.getStudent().getParentName())
                    .messageType(MessageType.LEVEL_TEST)
                    .content(content)
                    .sendStatus("PENDING")
                    .build();

            Message savedMessage = messageRepository.save(message);
            savedMessage.markAsSent(LocalDateTime.now(), "AUTO-LEVELTEST-" + savedMessage.getId());

            // 알림 발송 표시
            test.markNotificationSent();

            log.info("레벨테스트 전날 알림 발송: 학생={}, 테스트일={}",
                    test.getStudent().getStudentName(), test.getTestDate());
        }

        log.info("레벨테스트 전날 알림 발송 완료: 총 {}건", tomorrowTests.size());
    }

    /**
     * 최근 N일 이내 특정 타입의 알림을 발송했는지 확인
     */
    private boolean hasRecentNotification(Student student, MessageType messageType, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Message> recentMessages = messageRepository.findByStudentAndMessageTypeAndSentAtAfter(
                student, messageType, since);
        return !recentMessages.isEmpty();
    }
}
