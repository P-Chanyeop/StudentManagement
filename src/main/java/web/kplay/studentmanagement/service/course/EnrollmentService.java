package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.course.EnrollmentCreateRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final web.kplay.studentmanagement.service.holiday.HolidayService holidayService;

    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        // 기간+횟수 검증 (모든 수강권에 적용)
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("종료일은 시작일보다 이후여야 합니다");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalCount(request.getTotalCount())
                .usedCount(0)
                .remainingCount(request.getTotalCount())
                .isActive(true)
                .memo(request.getMemo())
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("새 수강권 등록: 학생={}, 수업={}, 기간={} ~ {}, 횟수={}/{}",
                student.getStudentName(), course.getCourseName(),
                request.getStartDate(), request.getEndDate(),
                request.getTotalCount(), request.getTotalCount());

        return toResponse(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getAllEnrollments() {
        return enrollmentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getActiveEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentIdAndIsActive(studentId, true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getExpiringEnrollments(int days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);
        return enrollmentRepository.findExpiringEnrollments(now, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getLowCountEnrollments(int threshold) {
        return enrollmentRepository.findLowCountEnrollments(threshold).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnrollmentResponse extendPeriod(Long id, LocalDate newEndDate) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));

        enrollment.extendPeriod(newEndDate);
        log.info("수강권 기간 연장: 수강권ID={}, 새종료일={}", id, newEndDate);
        return toResponse(enrollment);
    }

    /**
     * 공휴일을 제외한 N일 후로 수강권 기간 연장
     * 예: 30일권을 구매하면 주말과 공휴일을 제외한 실제 수업일 30일 보장
     */
    @Transactional
    public EnrollmentResponse extendPeriodWithHolidays(Long id, int businessDays) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));

        LocalDate currentEndDate = enrollment.getEndDate();
        LocalDate newEndDate = holidayService.addBusinessDays(currentEndDate, businessDays);

        enrollment.extendPeriod(newEndDate);
        log.info("수강권 기간 연장 (공휴일 제외): 수강권ID={}, 추가일수={}, 새종료일={}",
                id, businessDays, newEndDate);

        return toResponse(enrollment);
    }

    /**
     * 공휴일을 제외한 실제 수업일 기준으로 수강권 생성
     * 모든 수강권은 기간 + 횟수를 함께 가짐
     */
    @Transactional
    public EnrollmentResponse createEnrollmentWithHolidays(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        // 공휴일을 제외한 실제 종료일 계산
        LocalDate endDate;
        if (request.getEndDate() != null) {
            // 종료일이 직접 지정된 경우
            endDate = request.getEndDate();
        } else {
            // 시작일 + 수업일수로 공휴일 제외하여 종료일 계산
            endDate = holidayService.addBusinessDays(request.getStartDate(), request.getTotalCount());
            log.info("공휴일 제외 종료일 계산: 시작={}, 수업일수={}, 종료={}",
                    request.getStartDate(), request.getTotalCount(), endDate);
        }

        if (endDate.isBefore(request.getStartDate())) {
            throw new BusinessException("종료일은 시작일보다 이후여야 합니다");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(request.getStartDate())
                .endDate(endDate)
                .totalCount(request.getTotalCount())
                .usedCount(0)
                .remainingCount(request.getTotalCount())
                .isActive(true)
                .memo(request.getMemo())
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("새 수강권 등록 (공휴일 고려): 학생={}, 수업={}, 기간={} ~ {}, 횟수={}/{}",
                student.getStudentName(), course.getCourseName(),
                request.getStartDate(), endDate,
                request.getTotalCount(), request.getTotalCount());

        return toResponse(savedEnrollment);
    }

    @Transactional
    public EnrollmentResponse addCount(Long id, Integer additionalCount) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));

        enrollment.addCount(additionalCount);
        log.info("수강권 횟수 추가: 수강권ID={}, 추가횟수={}", id, additionalCount);
        return toResponse(enrollment);
    }

    @Transactional
    public void deactivateEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        enrollment.deactivate();
        log.info("수강권 비활성화: 수강권ID={}", id);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getStudentName())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getCourseName())
                .startDate(enrollment.getStartDate())
                .endDate(enrollment.getEndDate())
                .totalCount(enrollment.getTotalCount())
                .usedCount(enrollment.getUsedCount())
                .remainingCount(enrollment.getRemainingCount())
                .isActive(enrollment.getIsActive())
                .memo(enrollment.getMemo())
                .build();
    }
}
