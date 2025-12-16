package web.kplay.studentmanagement.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        // User 생성
        String username = generateUsername(request.getStudentName());
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("student123")) // 기본 비밀번호
                .name(request.getStudentName())
                .phoneNumber(request.getStudentPhone())
                .email(request.getParentEmail())
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
        userRepository.save(user);

        // Student 생성
        Student student = Student.builder()
                .user(user)
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
        log.info("새 학생 등록: {}", savedStudent.getStudentName());

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
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getActiveStudents() {
        return studentRepository.findByIsActive(true).stream()
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
    public void deactivateStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));
        student.deactivate();
        student.getUser().deactivate();
        log.info("학생 비활성화: {}", student.getStudentName());
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
                .build();
    }

    private String generateUsername(String name) {
        String baseUsername = name.replaceAll("\\s+", "").toLowerCase();
        String username = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }

        return username;
    }
}
