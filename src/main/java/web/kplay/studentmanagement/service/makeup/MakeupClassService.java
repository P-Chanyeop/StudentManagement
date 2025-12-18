package web.kplay.studentmanagement.service.makeup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.makeup.MakeupClass;
import web.kplay.studentmanagement.domain.makeup.MakeupStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.makeup.MakeupClassCreateRequest;
import web.kplay.studentmanagement.dto.makeup.MakeupClassResponse;
import web.kplay.studentmanagement.dto.makeup.MakeupClassUpdateRequest;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.MakeupClassRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakeupClassService {

    private final MakeupClassRepository makeupClassRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public MakeupClassResponse createMakeupClass(MakeupClassCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        MakeupClass makeupClass = MakeupClass.builder()
                .student(student)
                .course(course)
                .originalDate(request.getOriginalDate())
                .makeupDate(request.getMakeupDate())
                .makeupTime(request.getMakeupTime())
                .reason(request.getReason())
                .status(MakeupStatus.SCHEDULED)
                .memo(request.getMemo())
                .build();

        MakeupClass saved = makeupClassRepository.save(makeupClass);
        log.info("새 보강 수업 등록: 학생={}, 수업={}, 보강일={}",
                student.getStudentName(), course.getCourseName(), request.getMakeupDate());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MakeupClassResponse getMakeupClass(Long id) {
        MakeupClass makeupClass = makeupClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("보강 수업을 찾을 수 없습니다"));
        return toResponse(makeupClass);
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getAllMakeupClasses() {
        return makeupClassRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getMakeupClassesByStudent(Long studentId) {
        return makeupClassRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getMakeupClassesByCourse(Long courseId) {
        return makeupClassRepository.findByCourseId(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getMakeupClassesByDateRange(LocalDate startDate, LocalDate endDate) {
        return makeupClassRepository.findByMakeupDateBetween(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getMakeupClassesByDate(LocalDate date) {
        return makeupClassRepository.findByMakeupDateOrderByMakeupTime(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getMakeupClassesByStatus(MakeupStatus status) {
        return makeupClassRepository.findByStatusOrderByMakeupDateDesc(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getUpcomingMakeupsByStudent(Long studentId) {
        return makeupClassRepository.findUpcomingMakeupsByStudent(studentId, LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakeupClassResponse> getAllUpcomingMakeups() {
        return makeupClassRepository.findAllUpcomingMakeups(LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MakeupClassResponse updateMakeupClass(Long id, MakeupClassUpdateRequest request) {
        MakeupClass makeupClass = makeupClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("보강 수업을 찾을 수 없습니다"));

        if (request.getMakeupDate() != null && request.getMakeupTime() != null) {
            makeupClass.reschedule(request.getMakeupDate(), request.getMakeupTime());
        }

        if (request.getStatus() != null) {
            makeupClass.updateStatus(request.getStatus());
        }

        if (request.getMemo() != null) {
            makeupClass.updateMemo(request.getMemo());
        }

        log.info("보강 수업 수정: ID={}, 상태={}", id, request.getStatus());
        return toResponse(makeupClass);
    }

    @Transactional
    public void deleteMakeupClass(Long id) {
        if (!makeupClassRepository.existsById(id)) {
            throw new ResourceNotFoundException("보강 수업을 찾을 수 없습니다");
        }
        makeupClassRepository.deleteById(id);
        log.info("보강 수업 삭제: ID={}", id);
    }

    @Transactional
    public MakeupClassResponse completeMakeupClass(Long id) {
        MakeupClass makeupClass = makeupClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("보강 수업을 찾을 수 없습니다"));

        makeupClass.updateStatus(MakeupStatus.COMPLETED);
        log.info("보강 수업 완료 처리: ID={}", id);
        return toResponse(makeupClass);
    }

    @Transactional
    public MakeupClassResponse cancelMakeupClass(Long id) {
        MakeupClass makeupClass = makeupClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("보강 수업을 찾을 수 없습니다"));

        makeupClass.updateStatus(MakeupStatus.CANCELLED);
        log.info("보강 수업 취소 처리: ID={}", id);
        return toResponse(makeupClass);
    }

    private MakeupClassResponse toResponse(MakeupClass makeupClass) {
        return MakeupClassResponse.builder()
                .id(makeupClass.getId())
                .studentId(makeupClass.getStudent().getId())
                .studentName(makeupClass.getStudent().getStudentName())
                .courseId(makeupClass.getCourse().getId())
                .courseName(makeupClass.getCourse().getCourseName())
                .originalDate(makeupClass.getOriginalDate())
                .makeupDate(makeupClass.getMakeupDate())
                .makeupTime(makeupClass.getMakeupTime())
                .reason(makeupClass.getReason())
                .status(makeupClass.getStatus())
                .memo(makeupClass.getMemo())
                .build();
    }
}
