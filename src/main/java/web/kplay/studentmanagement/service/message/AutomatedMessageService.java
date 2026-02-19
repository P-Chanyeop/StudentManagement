package web.kplay.studentmanagement.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.leveltest.LevelTest;
import web.kplay.studentmanagement.domain.message.Message;
import web.kplay.studentmanagement.domain.message.MessageType;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.LevelTestRepository;
import web.kplay.studentmanagement.repository.MessageRepository;
import web.kplay.studentmanagement.service.message.sms.SmsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 자동화된 문자 메시지 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatedMessageService {

    private final MessageRepository messageRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LevelTestRepository levelTestRepository;
    private final SmsService smsService;
    private final web.kplay.studentmanagement.repository.StudentRepository studentRepository;
    private final web.kplay.studentmanagement.repository.ConsultationRepository consultationRepository;
    private final web.kplay.studentmanagement.repository.NaverBookingRepository naverBookingRepository;
    
    @Value("${app.homepage-url:https://littlebear.kplay.web}")
    private String homepageUrl;

    /**
     * 1. 첫등록 후 기간안내 문자 발송
     */
    @Transactional
    public void sendEnrollmentRegisterNotification(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        LocalDate startDate = enrollment.getStartDate();
        LocalDate endDate = enrollment.getEndDate();
        int totalCount = enrollment.getTotalCount();
        
        // 레코딩 횟수 계산 (6회에 1회)
        int recordingCount = totalCount / 6;
        
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s학생의 수강 유효기한은\n%d회에 대해 %d/%d - %d/%d까지 유효합니다.\n\n" +
                "아래 리틀베어 리딩클럽 학원 홈페이지를 통해서\n" +
                "아이 출결 사항 및 레코딩 파일 들어보실 수 있습니다:)\n\n" +
                "%s 통해 가입 진행 부탁드립니다.\n\n" +
                "레코딩은 6회에 1회씩 진행되며\n이번 텀은 총 %d회 업로드 될 예정입니다.\n\n" +
                "문의사항 있으시면 말씀해주세요! 감사합니다! :)",
                student.getStudentName(),
                totalCount,
                startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getMonthValue(), endDate.getDayOfMonth(),
                homepageUrl,
                recordingCount
        );

        sendAndSaveMessage(student, MessageType.ENROLLMENT_REGISTER, content);
        log.info("첫등록 안내 문자 발송: 학생={}, 기간={} ~ {}", student.getStudentName(), startDate, endDate);
    }

    /**
     * 예약 확인 알림 발송
     */
    @Transactional
    public void sendReservationNotification(web.kplay.studentmanagement.domain.reservation.Reservation reservation) {
        Student student = reservation.getStudent();
        String reservationType = reservation.getConsultationType() != null ? "상담" : "수업";
        
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생의 %s 예약이 완료되었습니다.\n날짜: %s\n시간: %s\n\n감사합니다! :)",
                student.getStudentName(),
                reservationType,
                reservation.getReservationDate(),
                reservation.getReservationTime().toString().substring(0, 5)
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("예약 알림 발송: 학생={}, 날짜={}", student.getStudentName(), reservation.getReservationDate());
    }

    /**
     * 상담 예약 확인 알림 발송
     */
    @Transactional
    public void sendConsultationNotification(web.kplay.studentmanagement.domain.consultation.Consultation consultation) {
        // 녹음파일이 없으면 문자 발송 안함
        if (consultation.getRecordingFileUrl() == null || consultation.getRecordingFileUrl().isEmpty()) {
            return;
        }
        
        Student student = consultation.getStudent();
        
        // 해당 학생의 녹음파일 있는 학습기록 개수 (회차)
        int recordingCount = consultationRepository.countByStudentIdAndRecordingFileUrlIsNotNull(student.getId());
        
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생의 %d회차 레코딩 파일 업로드되었습니다.\n" +
                "학습현황 탭에서 확인 가능합니다.\n감사합니다! :)",
                student.getStudentName(),
                recordingCount
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("레코딩 업로드 알림 발송: 학생={}, 회차={}", student.getStudentName(), recordingCount);
    }

    /**
     * 등원 알림 발송
     */
    @Transactional
    public void sendCheckInNotification(Student student, LocalDateTime checkInTime, LocalTime expectedLeaveTime) {
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 %d/%d %d:%02d 등원했습니다.\n\n감사합니다! :)",
                student.getStudentName(),
                checkInTime.getMonthValue(),
                checkInTime.getDayOfMonth(),
                checkInTime.getHour(),
                checkInTime.getMinute()
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("등원 알림 발송: 학생={}, 등원시간={}", student.getStudentName(), checkInTime.toLocalTime());
    }

    /**
     * 하원 알림 발송
     */
    @Transactional
    public void sendCheckOutNotification(Student student, LocalDateTime checkOutTime) {
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 %d/%d %d:%02d 하원했습니다.\n\n감사합니다! :)",
                student.getStudentName(),
                checkOutTime.getMonthValue(),
                checkOutTime.getDayOfMonth(),
                checkOutTime.getHour(),
                checkOutTime.getMinute()
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("하원 알림 발송: 학생={}, 하원시간={}", student.getStudentName(), checkOutTime.toLocalTime());
    }

    /**
     * 네이버 예약 등원 알림 발송
     */
    @Transactional
    public void sendNaverCheckInNotification(NaverBooking booking, LocalDateTime checkInTime, LocalTime expectedLeaveTime) {
        String studentName = booking.getStudentName() != null ? booking.getStudentName() : booking.getName();
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 %d/%d %d:%02d 등원했습니다.\n\n감사합니다! :)",
                studentName,
                checkInTime.getMonthValue(),
                checkInTime.getDayOfMonth(),
                checkInTime.getHour(),
                checkInTime.getMinute()
        );

        String phone = booking.getPhone();
        if (phone != null && !phone.isEmpty()) {
            sendAndSaveNaverMessage(booking, MessageType.GENERAL, content);
            log.info("네이버 예약 등원 알림 발송: 학생={}, 전화={}", studentName, phone);
        }
    }

    /**
     * 네이버 예약 하원 알림 발송
     */
    @Transactional
    public void sendNaverCheckOutNotification(NaverBooking booking, LocalDateTime checkOutTime) {
        String studentName = booking.getStudentName() != null ? booking.getStudentName() : booking.getName();
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 %d/%d %d:%02d 하원했습니다.\n\n감사합니다! :)",
                studentName,
                checkOutTime.getMonthValue(),
                checkOutTime.getDayOfMonth(),
                checkOutTime.getHour(),
                checkOutTime.getMinute()
        );

        String phone = booking.getPhone();
        if (phone != null && !phone.isEmpty()) {
            sendAndSaveNaverMessage(booking, MessageType.GENERAL, content);
            log.info("네이버 예약 하원 알림 발송: 학생={}, 전화={}", studentName, phone);
        }
    }

    /**
     * 네이버 예약 미출석 알림 발송 (15분 경과)
     */
    @Transactional
    public void sendNaverNoShowNotification(NaverBooking booking, LocalTime scheduledTime) {
        String studentName = booking.getStudentName() != null ? booking.getStudentName() : booking.getName();
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 오늘 %d시 예약되어있는데 아직 등원하지 않아 연락드립니다.\n" +
                "당일 결석의 경우 횟수 차감인점 안내드리며 확인 부탁드립니다.\n\n감사합니다! :)",
                studentName,
                scheduledTime.getHour()
        );

        String phone = booking.getPhone();
        if (phone != null && !phone.isEmpty()) {
            sendAndSaveNaverMessage(booking, MessageType.GENERAL, content);
            log.info("네이버 예약 미출석 알림 발송: 학생={}, 예약시간={}", studentName, scheduledTime);
        }
    }

    /**
     * 레코딩 업로드 완료 알림 발송
     */
    @Transactional
    public void sendRecordingUploadNotification(Student student, Integer sessionNumber) {
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생의 %d회차 레코딩 파일 업로드되었습니다.\n" +
                "학습현황 탭에서 확인 가능합니다.\n감사합니다! :)",
                student.getStudentName(),
                sessionNumber
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("레코딩 업로드 알림 발송: 학생={}, 회차={}", student.getStudentName(), sessionNumber);
    }

    /**
     * 미출석 알림 발송 (15분 경과)
     */
    @Transactional
    public void sendNoShowNotification(Student student, LocalTime scheduledTime) {
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 오늘 %d시 예약되어있는데 아직 등원하지 않아 연락드립니다.\n" +
                "당일 결석의 경우 횟수 차감인점 안내드리며 확인 부탁드립니다.\n\n감사합니다! :)",
                student.getStudentName(),
                scheduledTime.getHour()
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("미출석 알림 발송: 학생={}, 예약시간={}", student.getStudentName(), scheduledTime);
    }

    /**
     * 홀딩 적용 알림 발송
     */
    @Transactional
    public void sendHoldNotification(Student student, LocalDate holdStart, LocalDate holdEnd, 
                                      LocalDate enrollStart, LocalDate enrollEnd) {
        // 홀딩 종료 다음날부터 등원 가능
        LocalDate resumeDate = holdEnd.plusDays(1);
        
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생 %d/%d - %d/%d 동안 홀딩 적용하여 수강 유효 기간은 %d/%d - %d/%d 까지로 변경 되었습니다. " +
                "출석일은 %d/%d 부터 등원 가능한점 안내드립니다.\n\n감사합니다! :)",
                student.getStudentName(),
                holdStart.getMonthValue(), holdStart.getDayOfMonth(),
                holdEnd.getMonthValue(), holdEnd.getDayOfMonth(),
                enrollStart.getMonthValue(), enrollStart.getDayOfMonth(),
                enrollEnd.getMonthValue(), enrollEnd.getDayOfMonth(),
                resumeDate.getMonthValue(), resumeDate.getDayOfMonth()
        );

        sendAndSaveMessage(student, MessageType.GENERAL, content);
        log.info("홀딩 알림 발송: 학생={}, 홀딩기간={} ~ {}", student.getStudentName(), holdStart, holdEnd);
    }

    /**
     * 공지사항 등록 알림 발송 (전체 학생 + 네이버 예약 학생)
     */
    @Transactional
    public void sendNoticeNotificationToAll(List<Student> students, List<NaverBooking> naverBookings) {
        String content = "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "리틀베어 홈페이지에 새로운 공지가 등록되었습니다.\n" +
                "공지사항을 확인해 주세요.\n\n감사합니다! :)";

        int count = 0;
        // 시스템 학생
        for (Student student : students) {
            if (student.getParentPhone() != null && !student.getParentPhone().isEmpty()) {
                sendAndSaveMessage(student, MessageType.GENERAL, content);
                count++;
            }
        }
        // 네이버 예약 학생 (중복 번호 제거)
        java.util.Set<String> sentPhones = students.stream()
                .map(Student::getParentPhone)
                .filter(p -> p != null && !p.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        
        for (NaverBooking booking : naverBookings) {
            String phone = booking.getPhone();
            if (phone != null && !phone.isEmpty() && !sentPhones.contains(phone)) {
                sendAndSaveNaverMessage(booking, MessageType.GENERAL, content);
                sentPhones.add(phone);
                count++;
            }
        }
        log.info("공지 알림 발송 완료: {}명", count);
    }

    /**
     * 공지사항 등록 알림 발송 (시스템 학생만 - 기존 호환)
     */
    @Transactional
    public void sendNoticeNotificationToAll(List<Student> students) {
        sendNoticeNotificationToAll(students, java.util.Collections.emptyList());
    }

    /**
     * 재원생 예약 시작 알림 (격주 일요일 오전 8시 30분)
     */
    @Scheduled(cron = "0 30 8 * * SUN")
    @Transactional
    public void sendReservationOpenNotification() {
        // 격주 확인
        LocalDate now = LocalDate.now();
        LocalDate baseDate = LocalDate.of(2024, 1, 7);
        long weeksBetween = ChronoUnit.WEEKS.between(baseDate, now);
        
        if (weeksBetween % 2 != 0) {
            log.info("격주가 아니므로 예약 알림을 발송하지 않습니다.");
            return;
        }

        String content = "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "30분 뒤 2주간 재원생 수업 예약창이 열립니다.\n" +
                "수업 예약 진행 부탁드립니다.\n\n감사합니다! :)";

        // 활성 수강권이 있는 학생들에게만 발송
        List<Student> activeStudents = enrollmentRepository.findAll().stream()
                .filter(e -> e.getIsActive() != null && e.getIsActive())
                .map(e -> e.getStudent())
                .distinct()
                .toList();

        int count = 0;
        for (Student student : activeStudents) {
            if (student.getParentPhone() != null && !student.getParentPhone().isEmpty()) {
                sendAndSaveMessage(student, MessageType.GENERAL, content);
                count++;
            }
        }
        log.info("재원생 예약 알림 발송 완료: {}명", count);
    }

    /**
     * 지각 자동 알림 발송
     * Attendance check-in 시 지각(10분 이상) 판정된 경우 자동 호출
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
     * 레벨테스트 전날 알림 (매일 오후 7시 실행)
     */
    @Scheduled(cron = "0 0 19 * * *") // 매일 오후 7시
    @Transactional
    public void sendLevelTestReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int count = 0;

        // 1. 시스템 레벨테스트 조회
        List<LevelTest> tomorrowTests = levelTestRepository.findByTestDate(tomorrow);

        for (LevelTest test : tomorrowTests) {
            if ("CANCELLED".equals(test.getTestStatus())) continue;
            if (test.getMessageNotificationSent()) continue;

            String timeStr = test.getTestTime() != null ? 
                    test.getTestTime().getHour() + "시" : "예정";

            String content = String.format(
                    "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                    "%s학생\n%d/%d %s에 레벨테스트 예정되어있습니다.\n\n" +
                    "스케줄 변동 있으시면 편하게 말씀해주세요.\n감사합니다 :)",
                    test.getStudent().getStudentName(),
                    tomorrow.getMonthValue(), tomorrow.getDayOfMonth(),
                    timeStr
            );

            sendAndSaveMessage(test.getStudent(), MessageType.LEVEL_TEST_REMINDER, content);
            test.markNotificationSent();
            count++;
            log.info("레벨테스트 전날 알림 발송: 학생={}, 테스트일={}",
                    test.getStudent().getStudentName(), test.getTestDate());
        }

        // 2. 네이버 예약 레벨테스트 조회
        String tomorrowStr = tomorrow.toString(); // "2026-02-20"
        List<NaverBooking> naverBookings = naverBookingRepository.findAll().stream()
                .filter(nb -> nb.getBookingTime() != null && nb.getBookingTime().startsWith(tomorrowStr))
                .filter(nb -> nb.getProduct() != null && nb.getProduct().contains("레벨테스트"))
                .filter(nb -> nb.getStatus() == null || !nb.getStatus().contains("취소"))
                .toList();

        for (NaverBooking booking : naverBookings) {
            String studentName = booking.getStudentName() != null ? booking.getStudentName() : booking.getName();
            String timeStr = "예정";
            try {
                String[] parts = booking.getBookingTime().split(" ");
                if (parts.length > 1) {
                    timeStr = parts[1].split(":")[0] + "시";
                }
            } catch (Exception ignored) {}

            String content = String.format(
                    "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                    "%s학생\n%d/%d %s에 레벨테스트 예정되어있습니다.\n\n" +
                    "스케줄 변동 있으시면 편하게 말씀해주세요.\n감사합니다 :)",
                    studentName,
                    tomorrow.getMonthValue(), tomorrow.getDayOfMonth(),
                    timeStr
            );

            sendAndSaveNaverMessage(booking, MessageType.LEVEL_TEST_REMINDER, content);
            count++;
            log.info("네이버 레벨테스트 전날 알림 발송: 학생={}, 테스트일={}",
                    studentName, tomorrow);
        }

        log.info("레벨테스트 전날 알림 발송 완료: 총 {}건", count);
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

    /**
     * 공통: 문자 저장 및 실제 발송
     */
    private void sendAndSaveMessage(Student student, MessageType messageType, String content) {
        Message message = Message.builder()
                .student(student)
                .recipientPhone(student.getParentPhone())
                .recipientName(student.getParentName())
                .messageType(messageType)
                .content(content)
                .sendStatus("PENDING")
                .build();

        Message savedMessage = messageRepository.save(message);
        
        // 실제 SMS 발송
        try {
            String externalId = smsService.sendSms(student.getParentPhone(), content);
            savedMessage.markAsSent(LocalDateTime.now(), externalId);
            log.info("SMS 발송 성공: 수신자={}, 타입={}", student.getParentPhone(), messageType);
        } catch (Exception e) {
            savedMessage.markAsFailed(e.getMessage());
            log.error("SMS 발송 실패: {}", e.getMessage());
        }
    }

    private void sendAndSaveNaverMessage(NaverBooking booking, MessageType messageType, String content) {
        String studentName = booking.getStudentName() != null ? booking.getStudentName() : booking.getName();
        Message message = Message.builder()
                .recipientPhone(booking.getPhone())
                .recipientName(studentName)
                .messageType(messageType)
                .content(content)
                .sendStatus("PENDING")
                .build();

        Message savedMessage = messageRepository.save(message);
        
        try {
            String externalId = smsService.sendSms(booking.getPhone(), content);
            savedMessage.markAsSent(LocalDateTime.now(), externalId);
            log.info("네이버 SMS 발송 성공: 수신자={}", booking.getPhone());
        } catch (Exception e) {
            savedMessage.markAsFailed(e.getMessage());
            log.error("네이버 SMS 발송 실패: {}", e.getMessage());
        }
    }

    /**
     * 수강 횟수 소진 알림 발송
     */
    @Transactional
    public void sendEnrollmentDepletedNotification(Student student) {
        String content = String.format(
                "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                "%s 학생의 횟수가 모두 소진되었습니다.\n\n" +
                "재등록을 원하신다면 결제 부탁드립니다.\n감사합니다! :)",
                student.getStudentName()
        );

        sendAndSaveMessage(student, MessageType.ENROLLMENT_EXPIRY, content);
        log.info("수강 횟수 소진 알림 발송: 학생={}", student.getStudentName());
    }

    /**
     * 수강 기간 완료 알림 (매일 오후 8시 실행)
     */
    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void sendEnrollmentCompletedNotifications() {
        LocalDate today = LocalDate.now();
        List<Enrollment> completedEnrollments = enrollmentRepository.findByEndDateAndIsActiveTrue(today);

        for (Enrollment enrollment : completedEnrollments) {
            String content = String.format(
                    "안녕하세요.\n리틀베어 리딩클럽입니다.\n\n" +
                    "%s 학생의 수강 기간이 완료되었습니다.\n\n" +
                    "재등록을 원하신다면 결제 부탁드립니다.\n감사합니다! :)",
                    enrollment.getStudent().getStudentName()
            );

            sendAndSaveMessage(enrollment.getStudent(), MessageType.ENROLLMENT_EXPIRY, content);
            log.info("수강 기간 완료 알림 발송: 학생={}, 종료일={}", 
                    enrollment.getStudent().getStudentName(), enrollment.getEndDate());
        }

        log.info("수강 기간 완료 알림 발송 완료: 총 {}건", completedEnrollments.size());
    }
}
