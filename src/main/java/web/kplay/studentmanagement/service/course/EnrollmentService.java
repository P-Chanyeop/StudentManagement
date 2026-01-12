package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.enrollment.EnrollmentAdjustment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.dto.course.EnrollmentCreateRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
import web.kplay.studentmanagement.dto.course.CourseResponse;
import web.kplay.studentmanagement.dto.student.StudentResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.repository.enrollment.EnrollmentAdjustmentRepository;
import web.kplay.studentmanagement.repository.ConsultationRepository;

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
    private final UserRepository userRepository;
    private final EnrollmentAdjustmentRepository enrollmentAdjustmentRepository;
    private final ConsultationRepository consultationRepository;
    private final web.kplay.studentmanagement.service.holiday.HolidayService holidayService;

    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));
        }

        // 종료일 계산 (공휴일 제외)
        LocalDate endDate;
        if (request.getEndDate() != null) {
            endDate = request.getEndDate();
        } else {
            // 시작일 + 총 횟수로 공휴일 제외하여 종료일 계산
            endDate = holidayService.addBusinessDays(request.getStartDate(), request.getTotalCount());
            log.info("End date calculated excluding holidays: start={}, class days={}, end={}", 
                    request.getStartDate(), request.getTotalCount(), endDate);
        }

        // 기간+횟수 검증
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
        log.info("New enrollment registered: student={}, course={}, period={} ~ {}, count={}/{}",
                student.getStudentName(), course.getCourseName(),
                request.getStartDate(), endDate,
                request.getTotalCount(), request.getTotalCount());

        return toResponse(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        return toResponse(enrollment);
    }

    /**
     * 사용자별 수강권 조회 (학생 본인 또는 학부모의 자녀)
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        
        List<Enrollment> enrollments;
        
        if (user.getRole() == UserRole.PARENT) {
            // 학부모인 경우 자녀들의 수강권 조회
            List<Student> children = studentRepository.findByParentUser(user);
            enrollments = children.stream()
                    .flatMap(child -> enrollmentRepository.findByStudentAndIsActiveTrue(child).stream())
                    .toList();
        } else {
            // 관리자나 선생님은 빈 리스트 반환
            enrollments = List.of();
        }
        
        return enrollments.stream()
                .map(this::toResponse)
                .toList();
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
        log.info("Enrollment period extended: enrollmentId={}, new end date={}", id, newEndDate);
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
        log.info("Enrollment period extended (excluding holidays): enrollmentId={}, additional days={}, new end date={}",
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

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));
        }

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
    public void deactivateEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        enrollment.deactivate();
        log.info("수강권 비활성화: 수강권ID={}", id);
    }

    /**
     * 개별 수업 시간 설정
     */
    @Transactional
    public EnrollmentResponse setCustomDuration(Long enrollmentId, Integer durationMinutes) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강권을 찾을 수 없습니다: " + enrollmentId));

        // 유효성 검증
        if (durationMinutes != null && (durationMinutes < 30 || durationMinutes > 300)) {
            throw new IllegalArgumentException("수업 시간은 30분에서 300분 사이여야 합니다: " + durationMinutes);
        }

        enrollment.setCustomDuration(durationMinutes);
        return toResponse(enrollment);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        // 레코딩 파일 현황 계산
        int totalSessions = enrollment.getTotalCount();
        int expectedRecordings = totalSessions / 6; // 6회에 1회씩 레코딩
        
        // 해당 학생의 상담 기록 중 레코딩 파일이 있는 개수 조회
        int actualRecordings = consultationRepository.countByStudentIdAndRecordingFileUrlIsNotNull(
            enrollment.getStudent().getId()
        );
        
        String recordingStatus = actualRecordings + "/" + expectedRecordings;
        
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getStudentName())
                .student(StudentResponse.builder()
                        .id(enrollment.getStudent().getId())
                        .studentName(enrollment.getStudent().getStudentName())
                        .build())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getCourseName())
                .course(CourseResponse.builder()
                        .id(enrollment.getCourse().getId())
                        .courseName(enrollment.getCourse().getCourseName())
                        .build())
                .startDate(enrollment.getStartDate())
                .endDate(enrollment.getEndDate())
                .totalCount(enrollment.getTotalCount())
                .usedCount(enrollment.getUsedCount())
                .remainingCount(enrollment.getRemainingCount())
                .isActive(enrollment.getIsActive())
                .customDurationMinutes(enrollment.getCustomDurationMinutes())
                .actualDurationMinutes(enrollment.getActualDurationMinutes())
                .memo(enrollment.getMemo())
                .recordingStatus(recordingStatus)
                .expectedRecordings(expectedRecordings)
                .actualRecordings(actualRecordings)
                .build();
    }

    /**
     * 수강권 횟수 자동 차감 (편의 메서드)
     * @param enrollmentId 수강권 ID
     * @param reason 차감 사유
     */
    @Transactional
    public void deductCount(Long enrollmentId, String reason) {
        adjustEnrollmentCount(enrollmentId, EnrollmentAdjustment.AdjustmentType.DEDUCT, 1, reason, null);
    }

    /**
     * 수강권 횟수 통합 조정 메서드
     * @param enrollmentId 수강권 ID
     * @param adjustmentType 조정 유형 (DEDUCT, ADD, RESTORE)
     * @param countChange 변경할 횟수
     * @param reason 조정 사유
     * @param adminId 관리자 ID (자동 차감인 경우 null)
     * @throws ResourceNotFoundException 수강권 또는 관리자를 찾을 수 없는 경우
     * @throws BusinessException 관리자 권한이 없거나 차감할 횟수가 부족한 경우
     */
    @Transactional
    public void adjustEnrollmentCount(Long enrollmentId, EnrollmentAdjustment.AdjustmentType adjustmentType, 
                                    Integer countChange, String reason, Long adminId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));
        
        // 관리자 권한 확인 (자동 차감인 경우 제외)
        User admin = null;
        if (adminId != null) {
            admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("관리자를 찾을 수 없습니다"));
            
            if (!admin.getRole().equals(UserRole.ADMIN)) {
                throw new BusinessException("관리자 권한이 필요합니다");
            }
        }
        
        // 횟수 조정
        switch (adjustmentType) {
            case DEDUCT:
                if (enrollment.getRemainingCount() < countChange) {
                    throw new BusinessException("차감할 수 있는 횟수가 부족합니다");
                }
                for (int i = 0; i < countChange; i++) {
                    enrollment.useCount();
                }
                break;
            case ADD:
                enrollment.addCount(countChange);
                break;
            case RESTORE:
                enrollment.restoreCount(countChange);
                break;
        }
        
        // 조정 이력 저장 (관리자 수동 조정인 경우만)
        if (admin != null) {
            EnrollmentAdjustment adjustment = EnrollmentAdjustment.builder()
                    .enrollment(enrollment)
                    .adjustmentType(adjustmentType)
                    .countChange(countChange)
                    .reason(reason)
                    .admin(admin)
                    .build();
            
            enrollmentAdjustmentRepository.save(adjustment);
            
            log.info("수강권 횟수 수동 조정 - ID: {}, 유형: {}, 변경량: {}, 관리자: {}", 
                    enrollmentId, adjustmentType, countChange, admin.getUsername());
        } else {
            log.info("수강권 횟수 자동 차감 - ID: {}, 사유: {}", enrollmentId, reason);
        }
    }

    /**
     * 수강권 조정 이력 조회
     * @param enrollmentId 수강권 ID
     * @return 조정 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<EnrollmentAdjustment> getAdjustmentHistory(Long enrollmentId) {
        return enrollmentAdjustmentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollmentId);
    }

    /**
     * 수강권 활성화 (관리자 전용)
     */
    @Transactional
    public void activateEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강권을 찾을 수 없습니다."));
        
        enrollment.activate();
        enrollmentRepository.save(enrollment);
        log.info("수강권 활성화 - ID: {}", enrollmentId);
    }

    /**
     * 수강권 만료 처리 (관리자 전용)
     */
    @Transactional
    public void expireEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강권을 찾을 수 없습니다."));
        
        enrollment.expire();
        enrollmentRepository.save(enrollment);
        log.info("수강권 만료 처리 - ID: {}", enrollmentId);
    }

    /**
     * 수강권 강제 삭제 (관리자 전용)
     */
    @Transactional
    public void forceDeleteEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강권을 찾을 수 없습니다."));
        
        enrollmentRepository.delete(enrollment);
        log.info("수강권 강제 삭제 - ID: {}", enrollmentId);
    }
}
