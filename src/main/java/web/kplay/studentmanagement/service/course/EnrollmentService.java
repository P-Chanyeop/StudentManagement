package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.course.EnrollmentType;
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

    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        // 수강권 타입별 검증
        if (request.getEnrollmentType() == EnrollmentType.PERIOD) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new BusinessException("기간권은 시작일과 종료일이 필요합니다");
            }
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new BusinessException("종료일은 시작일보다 이후여야 합니다");
            }
        } else if (request.getEnrollmentType() == EnrollmentType.COUNT) {
            if (request.getTotalCount() == null || request.getTotalCount() < 1) {
                throw new BusinessException("횟수권은 1회 이상의 총 횟수가 필요합니다");
            }
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrollmentType(request.getEnrollmentType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalCount(request.getTotalCount())
                .usedCount(0)
                .remainingCount(request.getTotalCount())
                .isActive(true)
                .memo(request.getMemo())
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("새 수강권 등록: 학생={}, 수업={}, 타입={}",
                student.getStudentName(), course.getCourseName(), request.getEnrollmentType());

        return toResponse(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        return toResponse(enrollment);
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
        return enrollmentRepository.findExpiringEnrollments(EnrollmentType.PERIOD, now, endDate).stream()
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

        if (enrollment.getEnrollmentType() != EnrollmentType.PERIOD) {
            throw new BusinessException("기간권만 기간 연장이 가능합니다");
        }

        enrollment.extendPeriod(newEndDate);
        log.info("수강권 기간 연장: 수강권ID={}, 새종료일={}", id, newEndDate);
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse addCount(Long id, Integer additionalCount) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));

        if (enrollment.getEnrollmentType() != EnrollmentType.COUNT) {
            throw new BusinessException("횟수권만 횟수 추가가 가능합니다");
        }

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
                .enrollmentType(enrollment.getEnrollmentType())
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
