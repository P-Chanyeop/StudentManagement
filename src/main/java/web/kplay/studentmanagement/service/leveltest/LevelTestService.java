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
import java.util.List;
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
                .feedback(levelTest.getFeedback())
                .strengths(levelTest.getStrengths())
                .improvements(levelTest.getImprovements())
                .recommendedLevel(levelTest.getRecommendedLevel())
                .memo(levelTest.getMemo())
                .messageNotificationSent(levelTest.getMessageNotificationSent())
                .build();
    }
}
