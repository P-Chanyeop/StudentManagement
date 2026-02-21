package web.kplay.studentmanagement.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.dto.student.StudentCreateRequest;
import web.kplay.studentmanagement.dto.student.StudentResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        // Student 생성 (User 생성 없이)
        Student student = Student.builder()
                .studentName(request.getStudentName())
                .studentPhone(request.getStudentPhone())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .address(request.getAddress())
                .school(request.getSchool())
                .grade(request.getGrade())
                .englishLevel(request.getEnglishLevel())
                .memo(request.getMemo())
                .parentName(request.getParentName())
                .parentPhone(request.getParentPhone())
                .parentEmail(request.getParentEmail())
                .isActive(true)
                .build();

        Student savedStudent = studentRepository.save(student);
        log.info("New student registered: {}", savedStudent.getStudentName());

        return toResponse(savedStudent);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getParentPhone() != null && !s.getParentPhone().isEmpty())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getActiveStudents() {
        return studentRepository.findByIsActive(true).stream()
                .filter(s -> s.getParentPhone() != null && !s.getParentPhone().isEmpty())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getMyStudents(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        List<Student> students;
        if (user.getRole() == UserRole.PARENT) {
            // 부모님은 자녀 조회 (부모 전화번호로 매칭)
            students = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        } else {
            students = List.of();
        }
        
        return students.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String keyword) {
        return studentRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentCreateRequest request, Authentication authentication) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 학부모 권한 검증
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PARENT"))) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 학부모는 본인 자녀만 수정 가능
            if (!student.getParentPhone().equals(user.getPhoneNumber())) {
                throw new RuntimeException("본인 자녀의 정보만 수정할 수 있습니다.");
            }
        }

        // 학생 정보 업데이트
        student.updateInfo(
                request.getStudentName(),
                request.getStudentPhone(),
                request.getBirthDate(),
                request.getGender(),
                request.getAddress(),
                request.getSchool(),
                request.getGrade()
        );

        // 학부모 정보 업데이트
        student.updateParentInfo(
                request.getParentName(),
                request.getParentPhone(),
                request.getParentEmail()
        );

        // 영어 레벨 및 메모 업데이트
        student.updateEnglishLevel(request.getEnglishLevel());
        student.updateMemo(request.getMemo());

        log.info("Student info updated: {}", student.getStudentName());
        return toResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentCreateRequest request) {
        // 기존 메서드 유지 (하위 호환성)
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 학생 정보 업데이트
        student.updateInfo(
                request.getStudentName(),
                request.getStudentPhone(),
                request.getBirthDate(),
                request.getGender(),
                request.getAddress(),
                request.getSchool(),
                request.getGrade()
        );

        // 학부모 정보 업데이트
        student.updateParentInfo(
                request.getParentName(),
                request.getParentPhone(),
                request.getParentEmail()
        );

        // 영어 레벨 및 메모 업데이트
        student.updateEnglishLevel(request.getEnglishLevel());
        student.updateMemo(request.getMemo());

        log.info("Student info updated: {}", student.getStudentName());
        return toResponse(student);
    }

    @Transactional
    public void deactivateStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));
        student.deactivate();
        // student.getUser().deactivate(); // User 제거됨
        log.info("Student deactivated: {}", student.getStudentName());
    }

    private StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentName(student.getStudentName())
                .studentPhone(student.getStudentPhone())
                .birthDate(student.getBirthDate())
                .gender(student.getGender())
                .address(student.getAddress())
                .school(student.getSchool())
                .grade(student.getGrade())
                .englishLevel(student.getEnglishLevel())
                .memo(student.getMemo())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .parentEmail(student.getParentEmail())
                .isActive(student.getIsActive())
                .renaissanceUsername(student.getRenaissanceUsername())
                .assignedVocabulary(student.getAssignedVocabulary())
                .assignedSightword(student.getAssignedSightword())
                .assignedGrammar(student.getAssignedGrammar())
                .assignedPhonics(student.getAssignedPhonics())
                .enrollments(student.getEnrollments().stream()
                        .map(this::toEnrollmentResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private web.kplay.studentmanagement.dto.course.EnrollmentResponse toEnrollmentResponse(web.kplay.studentmanagement.domain.course.Enrollment enrollment) {
        return web.kplay.studentmanagement.dto.course.EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getStudentName())
                .courseId(enrollment.getCourse() != null ? enrollment.getCourse().getId() : null)
                .courseName(enrollment.getCourse() != null ? enrollment.getCourse().getCourseName() : null)
                .startDate(enrollment.getStartDate())
                .endDate(enrollment.getEndDate())
                .totalCount(enrollment.getTotalCount())
                .usedCount(enrollment.getUsedCount())
                .remainingCount(enrollment.getRemainingCount())
                .isActive(enrollment.getIsActive())
                .build();
    }

    // 추가수업 할당 학생 목록 조회
    @Transactional(readOnly = true)
    public List<web.kplay.studentmanagement.dto.student.StudentAdditionalClassResponse> getAllStudentsWithAdditionalClass() {
        return studentRepository.findByIsActiveTrue().stream()
                .filter(s -> s.getParentPhone() != null && !s.getParentPhone().isEmpty())
                .map(s -> {
                    String className = null;
                    if (s.getDefaultCourse() != null) {
                        className = s.getDefaultCourse().getCourseName();
                    } else if (!s.getEnrollments().isEmpty()) {
                        className = s.getEnrollments().stream()
                                .filter(e -> e.getIsActive() && e.getCourse() != null)
                                .map(e -> e.getCourse().getCourseName())
                                .findFirst().orElse(null);
                    }
                    return web.kplay.studentmanagement.dto.student.StudentAdditionalClassResponse.builder()
                            .id(s.getId())
                            .studentName(s.getStudentName())
                            .className(className)
                            .assignedVocabulary(s.getAssignedVocabulary())
                            .assignedSightword(s.getAssignedSightword())
                            .assignedGrammar(s.getAssignedGrammar())
                            .assignedPhonics(s.getAssignedPhonics())
                            .assignedClassInitials(s.getAssignedClassInitials())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 추가수업 할당 업데이트
    @Transactional
    public web.kplay.studentmanagement.dto.student.StudentAdditionalClassResponse updateAdditionalClass(
            Long studentId, web.kplay.studentmanagement.dto.student.StudentAdditionalClassRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new web.kplay.studentmanagement.exception.ResourceNotFoundException("학생을 찾을 수 없습니다."));
        
        student.updateAssignedClasses(
                request.getAssignedVocabulary(),
                request.getAssignedSightword(),
                request.getAssignedGrammar(),
                request.getAssignedPhonics()
        );
        
        String className = null;
        if (student.getDefaultCourse() != null) {
            className = student.getDefaultCourse().getCourseName();
        } else if (!student.getEnrollments().isEmpty()) {
            className = student.getEnrollments().stream()
                    .filter(e -> e.getIsActive() && e.getCourse() != null)
                    .map(e -> e.getCourse().getCourseName())
                    .findFirst().orElse(null);
        }
        
        return web.kplay.studentmanagement.dto.student.StudentAdditionalClassResponse.builder()
                .id(student.getId())
                .studentName(student.getStudentName())
                .className(className)
                .assignedVocabulary(student.getAssignedVocabulary())
                .assignedSightword(student.getAssignedSightword())
                .assignedGrammar(student.getAssignedGrammar())
                .assignedPhonics(student.getAssignedPhonics())
                .assignedClassInitials(student.getAssignedClassInitials())
                .build();
    }
}
