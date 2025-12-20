package web.kplay.studentmanagement.service.leveltest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.leveltest.LevelTest;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.leveltest.LevelTestRequest;
import web.kplay.studentmanagement.dto.leveltest.LevelTestResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.LevelTestRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelTestService {

    private final LevelTestRepository levelTestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Transactional
    public LevelTestResponse createLevelTest(LevelTestRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("선생님을 찾을 수 없습니다"));
        }

        LevelTest levelTest = LevelTest.builder()
                .student(student)
                .teacher(teacher)
                .testDate(request.getTestDate())
                .testTime(request.getTestTime())
                .testStatus("SCHEDULED")
                .memo(request.getMemo())
                .messageNotificationSent(false)
                .build();

        LevelTest savedLevelTest = levelTestRepository.save(levelTest);
        log.info("레벨테스트 등록: 학생={}, 날짜={}",
                student.getStudentName(), request.getTestDate());

        return toResponse(savedLevelTest);
    }

    @Transactional(readOnly = true)
    public List<LevelTestResponse> getAllLevelTests() {
        return levelTestRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LevelTestResponse getLevelTest(Long id) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));
        return toResponse(levelTest);
    }

    @Transactional
    public LevelTestResponse updateLevelTest(Long id, LevelTestRequest request) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));

        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("선생님을 찾을 수 없습니다"));
        }

        levelTest.updateDetails(teacher, request.getTestDate(), request.getTestTime(), request.getMemo());

        log.info("레벨테스트 수정: ID={}", id);
        return toResponse(levelTest);
    }

    @Transactional
    public LevelTestResponse completeLevelTest(Long id) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));

        levelTest.markAsCompleted();
        log.info("레벨테스트 완료: 학생={}", levelTest.getStudent().getStudentName());
        return toResponse(levelTest);
    }

    @Transactional
    public LevelTestResponse completeLevelTest(Long id, String testResult, String feedback,
                                                String strengths, String improvements, String recommendedLevel) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));

        levelTest.complete(testResult, feedback, strengths, improvements, recommendedLevel);
        log.info("레벨테스트 완료: 학생={}, 결과={}",
                levelTest.getStudent().getStudentName(), testResult);

        return toResponse(levelTest);
    }

    @Transactional
    public LevelTestResponse cancelLevelTest(Long id) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));

        levelTest.markAsCancelled();
        log.info("레벨테스트 취소: 학생={}", levelTest.getStudent().getStudentName());
        return toResponse(levelTest);
    }

    @Transactional
    public LevelTestResponse saveLevelTestResult(Long id, String level, Integer score, String feedback) {
        LevelTest levelTest = levelTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("레벨테스트를 찾을 수 없습니다"));

        levelTest.saveResult(level, score, feedback);

        log.info("레벨테스트 결과 저장: 학생={}, 레벨={}, 점수={}",
                levelTest.getStudent().getStudentName(), level, score);

        return toResponse(levelTest);
    }

    @Transactional(readOnly = true)
    public List<LevelTestResponse> getLevelTestsByDateRange(LocalDate startDate, LocalDate endDate) {
        return levelTestRepository.findByDateRange(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LevelTestResponse> getLevelTestsByStudent(Long studentId) {
        return levelTestRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 월별 레벨테스트 조회 (캘린더 뷰용)
     */
    @Transactional(readOnly = true)
    public List<LevelTestResponse> getLevelTestsByMonth(int year, int month) {
        return levelTestRepository.findByYearAndMonth(year, month).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 주간 레벨테스트 조회 (주간 뷰용)
     */
    @Transactional(readOnly = true)
    public List<LevelTestResponse> getLevelTestsByWeek(LocalDate weekStart, LocalDate weekEnd) {
        return levelTestRepository.findByWeek(weekStart, weekEnd).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 선생님별 레벨테스트 조회
     */
    @Transactional(readOnly = true)
    public List<LevelTestResponse> getLevelTestsByTeacher(Long teacherId) {
        return levelTestRepository.findByTeacherId(teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 예정된 레벨테스트 조회 (오늘 이후 + SCHEDULED 상태)
     */
    @Transactional(readOnly = true)
    public List<LevelTestResponse> getUpcomingTests() {
        return levelTestRepository.findUpcomingTests(LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 완료된 레벨테스트 조회 (COMPLETED 상태)
     */
    @Transactional(readOnly = true)
    public List<LevelTestResponse> getCompletedTests() {
        return levelTestRepository.findCompletedTests().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 레벨테스트 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate) {
        List<LevelTest> tests = levelTestRepository.findByDateRange(startDate, endDate);

        Map<String, Object> statistics = new HashMap<>();

        // 전체 테스트 수
        statistics.put("totalTests", tests.size());

        // 완료된 테스트 수
        long completedCount = tests.stream()
                .filter(test -> "COMPLETED".equals(test.getTestStatus()))
                .count();
        statistics.put("completedTests", completedCount);

        // 예정된 테스트 수
        long scheduledCount = tests.stream()
                .filter(test -> "SCHEDULED".equals(test.getTestStatus()))
                .count();
        statistics.put("scheduledTests", scheduledCount);

        // 완료율
        double completionRate = tests.isEmpty() ? 0.0 :
                (completedCount * 100.0) / tests.size();
        statistics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // 선생님별 테스트 수
        Map<String, Long> testsByTeacher = tests.stream()
                .filter(test -> test.getTeacher() != null)
                .collect(Collectors.groupingBy(
                        test -> test.getTeacher().getName(),
                        Collectors.counting()
                ));
        statistics.put("testsByTeacher", testsByTeacher);

        // 상태별 테스트 수
        Map<String, Long> testsByStatus = tests.stream()
                .collect(Collectors.groupingBy(
                        LevelTest::getTestStatus,
                        Collectors.counting()
                ));
        statistics.put("testsByStatus", testsByStatus);

        // 일별 테스트 수
        Map<String, Long> testsByDate = tests.stream()
                .collect(Collectors.groupingBy(
                        test -> test.getTestDate().toString(),
                        Collectors.counting()
                ));
        statistics.put("testsByDate", testsByDate);

        log.info("레벨테스트 통계 조회: 기간={} ~ {}, 전체={}, 완료={}",
                startDate, endDate, tests.size(), completedCount);

        return statistics;
    }

    private LevelTestResponse toResponse(LevelTest levelTest) {
        return LevelTestResponse.builder()
                .id(levelTest.getId())
                .studentId(levelTest.getStudent().getId())
                .studentName(levelTest.getStudent().getStudentName())
                .teacherId(levelTest.getTeacher() != null ? levelTest.getTeacher().getId() : null)
                .teacherName(levelTest.getTeacher() != null ? levelTest.getTeacher().getName() : null)
                .testDate(levelTest.getTestDate())
                .testTime(levelTest.getTestTime())
                .testStatus(levelTest.getTestStatus())
                .testResult(levelTest.getTestResult())
                .testScore(levelTest.getTestScore())
                .feedback(levelTest.getFeedback())
                .strengths(levelTest.getStrengths())
                .improvements(levelTest.getImprovements())
                .recommendedLevel(levelTest.getRecommendedLevel())
                .memo(levelTest.getMemo())
                .messageNotificationSent(levelTest.getMessageNotificationSent())
                .build();
    }
}
