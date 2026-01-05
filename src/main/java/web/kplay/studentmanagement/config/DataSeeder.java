package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.consultation.Consultation;
import web.kplay.studentmanagement.domain.notice.Notice;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 개발 환경용 초기 데이터 시더
 * - 관리자 계정
 * - 선생님 계정
 * - 테스트 학생 데이터
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ConsultationRepository consultationRepository;
    private final NoticeRepository noticeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final web.kplay.studentmanagement.service.holiday.HolidayService holidayService;

    @Bean
    @Profile("dev") // dev 프로파일에서만 실행
    public CommandLineRunner loadInitialData() {
        return args -> {
            log.info("=== Initial data loading started ===");

            // 관리자 계정 생성
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .name("관리자")
                        .email("admin@kplay.web")
                        .phoneNumber("010-1234-5678")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                log.info("✓ Admin account created (username: admin)");
            }

            // 선생님 계정 생성
            if (userRepository.findByUsername("teacher1").isEmpty()) {
                User teacher1 = User.builder()
                        .username("teacher1")
                        .password(passwordEncoder.encode("teacher123"))
                        .name("김영어")
                        .email("teacher1@kplay.web")
                        .phoneNumber("010-2345-6789")
                        .role(UserRole.TEACHER)
                        .isActive(true)
                        .build();
                userRepository.save(teacher1);
                log.info("✓ Teacher account created (username: teacher1)");
            }

            if (userRepository.findByUsername("teacher2").isEmpty()) {
                User teacher2 = User.builder()
                        .username("teacher2")
                        .password(passwordEncoder.encode("teacher123"))
                        .name("이수학")
                        .email("teacher2@kplay.web")
                        .phoneNumber("010-3456-7890")
                        .role(UserRole.TEACHER)
                        .isActive(true)
                        .build();
                userRepository.save(teacher2);
                log.info("✓ Teacher account created (username: teacher2)");
            }

            // 학부모 계정 생성
            if (userRepository.findByUsername("parent1").isEmpty()) {
                User parent1 = User.builder()
                        .username("parent1")
                        .password(passwordEncoder.encode("parent123"))
                        .name("박학부모")
                        .email("parent1@kplay.web")
                        .phoneNumber("010-4567-8901")
                        .role(UserRole.PARENT)
                        .isActive(true)
                        .build();
                userRepository.save(parent1);
                log.info("✓ Parent account created (username: parent1)");
            }

            // 테스트 학생 데이터 생성
            if (studentRepository.count() == 0) {
                // 학생 데이터는 User 없이 생성하므로 주석 처리
                /*
                // 학생1용 User 계정
                User studentUser1 = User.builder()
                        .username("student1")
                        .password(passwordEncoder.encode("student123"))
                        .name("홍길동")
                        .email("student1@kplay.web")
                        .phoneNumber("010-5678-9012")
                        .role(UserRole.PARENT) // STUDENT 제거됨
                        .isActive(true)
                        .build();
                studentUser1 = userRepository.save(studentUser1);
                */

                Student student1 = Student.builder()
                        // .user(studentUser1) // 제거
                        .studentName("홍길동")
                        .birthDate(LocalDate.of(2010, 3, 15))
                        .gender("FEMALE")
                        .studentPhone("010-5678-9012")
                        .parentPhone("010-4567-8901")
                        .parentName("박학부모")
                        .school("서울초등학교")
                        .grade("6")
                        .englishLevel("2.3")
                        .address("서울시 강남구 테헤란로 123")
                        .memo("영어 초급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student1);

                // 학생2용 User 계정
//                User studentUser2 = User.builder()
//                        .username("student2")
//                        .password(passwordEncoder.encode("student123"))
//                        .name("김민수")
//                        .email("student2@kplay.web")
//                        .phoneNumber("010-6789-0123")
//                        .role(UserRole.STUDENT)
//                        .isActive(true)
//                        .build();
//                studentUser2 = userRepository.save(studentUser2);

                Student student2 = Student.builder()
//                        .user(studentUser2)
                        .studentName("김민수")
                        .birthDate(LocalDate.of(2011, 7, 20))
                        .gender("MALE")
                        .studentPhone("010-6789-0123")
                        .parentPhone("010-7890-1234")
                        .parentName("김학부모")
                        .school("서울초등학교")
                        .grade("5")
                        .englishLevel("4.7")
                        .address("서울시 강남구 역삼동 456")
                        .memo("수학 중급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student2);

                // 학생3용 User 계정
//                User studentUser3 = User.builder()
//                        .username("student3")
//                        .password(passwordEncoder.encode("student123"))
//                        .name("이지은")
//                        .email("student3@kplay.web")
//                        .phoneNumber("010-7890-1234")
//                        .role(UserRole.STUDENT)
//                        .isActive(true)
//                        .build();
//                studentUser3 = userRepository.save(studentUser3);

                Student student3 = Student.builder()
//                        .user(studentUser3)
                        .studentName("이지은")
                        .birthDate(LocalDate.of(2012, 11, 5))
                        .gender("FEMALE")
                        .studentPhone("010-7890-1234")
                        .parentPhone("010-8901-2345")
                        .parentName("이학부모")
                        .school("한강초등학교")
                        .grade("4")
                        .englishLevel("7.2")
                        .address("서울시 서초구 반포동 789")
                        .memo("영어 중급반, 수학 초급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student3);

                // 추가 학생 4-10
                String[] names = {"박서준", "최유나", "정민호", "강하늘", "윤서아", "임재현", "송지우"};
                String[] genders = {"MALE", "FEMALE", "MALE", "FEMALE", "FEMALE", "MALE", "MALE"};
                String[] levels = {"1.5", "3.8", "2.1", "6.4", "4.9", "1.8", "5.3"};
                String[] schools = {"서울초등학교", "한강초등학교", "강남초등학교"};
                
                for (int i = 0; i < names.length; i++) {
//                    User studentUser = User.builder()
//                            .username("student" + (i + 4))
//                            .password(passwordEncoder.encode("student123"))
//                            .name(names[i])
//                            .email("student" + (i + 4) + "@kplay.web")
//                            .phoneNumber("010-" + String.format("%04d", 8000 + i) + "-" + String.format("%04d", 1234 + i))
//                            .role(UserRole.STUDENT)
//                            .isActive(true)
//                            .build();
//                    studentUser = userRepository.save(studentUser);

                    Student student = Student.builder()
//                            .user(studentUser)
                            .studentName(names[i])
                            .birthDate(LocalDate.of(2010 + (i % 3), (i % 12) + 1, (i % 28) + 1))
                            .gender(genders[i])
                            .studentPhone("010-" + String.format("%04d", 8000 + i) + "-" + String.format("%04d", 1234 + i))
                            .parentPhone("010-" + String.format("%04d", 9000 + i) + "-" + String.format("%04d", 2345 + i))
                            .parentName(names[i].substring(0, 1) + "학부모")
                            .school(schools[i % 3])
                            .grade(String.valueOf(4 + (i % 3)))
                            .englishLevel(levels[i])
                            .address("서울시 강남구 " + (i + 1) + "번지")
                            .memo("테스트 학생")
                            .isActive(true)
                            .build();
                    studentRepository.save(student);
                }

                log.info("✓ 10 test students created");
                
                // 기존 학생에 parentUser 연결
                User parent1 = userRepository.findByUsername("parent1").orElse(null);
                if (parent1 != null) {
                    List<Student> studentsToUpdate = studentRepository.findByParentPhone("010-4567-8901");
                    for (Student student : studentsToUpdate) {
                        if (student.getParentUser() == null) {
                            student.setParentUser(parent1);
                            studentRepository.save(student);
                            log.info("✓ Parent account linked to student {}", student.getStudentName());
                        }
                    }
                }
            }

            // 테스트 수업 및 스케줄 생성
            if (courseRepository.count() == 0) {
                // 선생님 조회
                User teacher = userRepository.findByUsername("teacher1").orElse(null);
                
                // 수업 생성
                Course englishCourse = Course.builder()
                        .courseName("초급 영어")
                        .description("초등학생 대상 기초 영어 수업")
                        .teacher(teacher)
                        .maxStudents(8)
                        .durationMinutes(120)
                        .level("초급")
                        .color("#4CAF50")
                        .isActive(true)
                        .build();
                courseRepository.save(englishCourse);

                // 오늘 스케줄 생성
                CourseSchedule todaySchedule = CourseSchedule.builder()
                        .course(englishCourse)
                        .scheduleDate(LocalDate.now())
                        .startTime(LocalTime.of(14, 0))
                        .endTime(LocalTime.of(16, 0))
                        .dayOfWeek(LocalDate.now().getDayOfWeek().name())
                        .currentStudents(0)
                        .isCancelled(false)
                        .build();
                scheduleRepository.save(todaySchedule);

                // 학생들에게 수강권 할당 (12주 기간 + 24회 사용 가능)
                List<Student> students = studentRepository.findAll();
                for (int i = 0; i < Math.min(5, students.size()); i++) {
                    Student student = students.get(i);
                    LocalDate startDate = LocalDate.now();
                    // 영업일 기준 12주 (60영업일) 계산
                    LocalDate endDate = holidayService.calculateEndDate(startDate, 60);
                    
                    Enrollment enrollment = Enrollment.builder()
                            .student(student)
                            .course(englishCourse)
                            .startDate(startDate)
                            .endDate(endDate)
                            .totalCount(24) // 12주 동안 24회 사용 가능
                            .usedCount(i * 2) // 학생별로 다른 사용 횟수
                            .remainingCount(24 - (i * 2))
                            .isActive(true)
                            .build();
                    enrollmentRepository.save(enrollment);
                }

                log.info("✓ Test courses and schedules created (Today 14:00-16:00)");
            }

            // 테스트 상담 데이터 생성
            if (consultationRepository.count() == 0) {
                User admin = userRepository.findByUsername("admin").orElse(null);
                User teacher = userRepository.findByUsername("teacher1").orElse(null);
                List<Student> students = studentRepository.findAll();

                if (admin != null && teacher != null && !students.isEmpty()) {
                    // 상담 1: 홍길동 - 학습 계획 수립
                    Consultation consultation1 = Consultation.builder()
                            .student(students.get(0))
                            .consultant(teacher)
                            .title("첫 상담 - 학습 계획 수립")
                            .content("홍길동 학생의 현재 영어 실력을 평가하고 향후 학습 계획을 논의했습니다. " +
                                    "기초 문법이 부족하여 기본기 강화가 필요합니다. " +
                                    "매일 단어 암기와 간단한 문장 만들기 연습을 권장합니다.")
                            .consultationType("학습상담")
                            .consultationDate(LocalDate.now().minusDays(7))
                            .actionItems("1. 기초 문법 교재 제공\n2. 매일 단어 10개씩 암기\n3. 주 2회 추가 과제 제공")
                            .nextConsultationDate(LocalDate.now().plusDays(7))
                            .build();
                    consultationRepository.save(consultation1);

                    // 상담 2: 홍길동 - 학부모 상담
                    Consultation consultation2 = Consultation.builder()
                            .student(students.get(0))
                            .consultant(admin)
                            .title("학부모 상담 - 진도 점검")
                            .content("홍길동 학생의 1주차 학습 진도를 점검했습니다. " +
                                    "단어 암기는 잘 하고 있으나 문법 적용에 어려움을 보입니다. " +
                                    "좀 더 체계적인 문법 학습이 필요합니다.")
                            .consultationType("학부모상담")
                            .consultationDate(LocalDate.now().minusDays(3))
                            .actionItems("1. 문법 기초 강화 수업 추가\n2. 가정에서 영어 일기 쓰기 지도")
                            .recordingFileUrl("/uploads/audio/consultation_001.mp3")
                            .build();
                    consultationRepository.save(consultation2);

                    // 상담 3: 김민수 - 중급 과정 진입
                    if (students.size() > 1) {
                        Consultation consultation3 = Consultation.builder()
                                .student(students.get(1))
                                .consultant(teacher)
                                .title("중급 과정 진입 상담")
                                .content("김민수 학생이 중급 과정으로 진입하면서 학습 방향을 조정했습니다. " +
                                        "읽기 실력은 우수하나 말하기에 자신감이 부족합니다. " +
                                        "회화 연습을 늘리고 발표 기회를 제공하기로 했습니다.")
                                .consultationType("학습상담")
                                .consultationDate(LocalDate.now().minusDays(5))
                                .actionItems("1. 주 1회 영어 발표 시간 마련\n2. 원어민 회화 수업 추가 검토\n3. 자신감 향상을 위한 격려")
                                .nextConsultationDate(LocalDate.now().plusDays(14))
                                .attachmentFileUrl("/uploads/documents/speaking_practice_plan.pdf")
                                .build();
                        consultationRepository.save(consultation3);
                    }

                    // 상담 4: 이지은 - 진로 상담
                    if (students.size() > 2) {
                        Consultation consultation4 = Consultation.builder()
                                .student(students.get(2))
                                .consultant(admin)
                                .title("영어 특기자 진로 상담")
                                .content("이지은 학생의 뛰어난 영어 실력을 바탕으로 특목고 진학과 " +
                                        "영어 인증시험 준비에 대해 상담했습니다. " +
                                        "TOEFL Junior 시험 준비를 시작하기로 결정했습니다.")
                                .consultationType("진로상담")
                                .consultationDate(LocalDate.now().minusDays(1))
                                .actionItems("1. TOEFL Junior 교재 준비\n2. 모의고사 일정 수립\n3. 고급 독해 자료 제공")
                                .nextConsultationDate(LocalDate.now().plusDays(10))
                                .recordingFileUrl("/uploads/audio/consultation_002.mp3")
                                .attachmentFileUrl("/uploads/documents/toefl_study_plan.pdf")
                                .build();
                        consultationRepository.save(consultation4);
                    }

                    // 상담 5: 김민수 - 생활 상담
                    if (students.size() > 1) {
                        Consultation consultation5 = Consultation.builder()
                                .student(students.get(1))
                                .consultant(teacher)
                                .title("학습 태도 개선 상담")
                                .content("최근 김민수 학생의 수업 참여도가 떨어지는 것에 대해 상담했습니다. " +
                                        "개인적인 고민이 있어 집중력이 저하된 것으로 파악됩니다. " +
                                        "학부모와의 추가 상담이 필요합니다.")
                                .consultationType("생활상담")
                                .consultationDate(LocalDate.now())
                                .actionItems("1. 학부모 면담 일정 조율\n2. 개별 학습 지도 강화\n3. 심리적 지원 방안 모색")
                                .nextConsultationDate(LocalDate.now().plusDays(3))
                                .build();
                        consultationRepository.save(consultation5);
                    }

                    log.info("✓ 5 test consultation records created");
                }
            }

            // 6. 테스트용 출석 데이터 생성 (비활성화)
            // createAttendanceRecords();

            // 테스트 공지사항 데이터 생성
            if (noticeRepository.count() == 0) {
                User admin = userRepository.findByUsername("admin").orElse(null);
                User teacher = userRepository.findByUsername("teacher1").orElse(null);

                if (admin != null && teacher != null) {
                    // 공지사항 1: 중요 공지 (상단 고정)
                    Notice notice1 = Notice.builder()
                            .title("📢 2025년 새학기 개강 안내")
                            .content("안녕하세요. 학부모님들께 새학기 개강 일정을 안내드립니다.\n\n" +
                                    "• 개강일: 2025년 3월 4일(월)\n" +
                                    "• 수업 시간: 기존과 동일\n" +
                                    "• 교재비: 별도 안내 예정\n\n" +
                                    "궁금한 사항이 있으시면 언제든 연락 주세요.")
                            .author(admin)
                            .isPinned(true)
                            .isActive(true)
                            .viewCount(45)
                            .build();
                    noticeRepository.save(notice1);

                    // 공지사항 2: 일반 공지
                    Notice notice2 = Notice.builder()
                            .title("겨울방학 특강 수강생 모집")
                            .content("겨울방학 동안 진행될 특강 프로그램 수강생을 모집합니다.\n\n" +
                                    "• 기간: 12월 26일 ~ 1월 31일\n" +
                                    "• 대상: 초등 3~6학년\n" +
                                    "• 과목: 영어 집중반, 수학 심화반\n" +
                                    "• 신청: 12월 20일까지\n\n" +
                                    "자세한 내용은 학원으로 문의해 주세요.")
                            .author(teacher)
                            .isPinned(false)
                            .isActive(true)
                            .viewCount(23)
                            .build();
                    noticeRepository.save(notice2);

                    // 공지사항 3: 시험 안내
                    Notice notice3 = Notice.builder()
                            .title("12월 정기 레벨테스트 안내")
                            .content("12월 정기 레벨테스트를 다음과 같이 실시합니다.\n\n" +
                                    "• 일시: 12월 28일(목) 오후 2시\n" +
                                    "• 대상: 전체 수강생\n" +
                                    "• 준비물: 필기구, 계산기\n" +
                                    "• 결과 발표: 1월 2일\n\n" +
                                    "시험 결과에 따라 반 편성이 조정될 수 있습니다.")
                            .author(admin)
                            .isPinned(true)
                            .isActive(true)
                            .viewCount(67)
                            .build();
                    noticeRepository.save(notice3);

                    // 공지사항 4: 휴원 안내
                    Notice notice4 = Notice.builder()
                            .title("연말연시 휴원 안내")
                            .content("연말연시 휴원 일정을 안내드립니다.\n\n" +
                                    "• 휴원 기간: 12월 30일(토) ~ 1월 2일(화)\n" +
                                    "• 정상 수업: 1월 3일(수)부터\n" +
                                    "• 보강 수업: 별도 공지 예정\n\n" +
                                    "새해 복 많이 받으세요!")
                            .author(admin)
                            .isPinned(false)
                            .isActive(true)
                            .viewCount(34)
                            .build();
                    noticeRepository.save(notice4);

                    // 공지사항 5: 학부모 상담 안내
                    Notice notice5 = Notice.builder()
                            .title("1월 학부모 개별 상담 신청 안내")
                            .content("자녀의 학습 상황을 점검하는 개별 상담을 진행합니다.\n\n" +
                                    "• 상담 기간: 1월 8일 ~ 1월 19일\n" +
                                    "• 상담 시간: 1회 30분\n" +
                                    "• 신청 방법: 전화 또는 방문 접수\n" +
                                    "• 상담 내용: 학습 진도, 성취도, 향후 계획\n\n" +
                                    "많은 참여 부탁드립니다.")
                            .author(teacher)
                            .isPinned(false)
                            .isActive(true)
                            .viewCount(18)
                            .build();
                    noticeRepository.save(notice5);

                    log.info("✓ 5 test notice records created");
                }
            }

            // 레벨테스트 및 일반 수업 Course 및 스케줄 생성
            createCoursesAndSchedules();

            log.info("=== Initial data loading completed ===");
            log.info("");
            log.info("📋 Initial accounts created (see CREDENTIALS.md for passwords)");
            log.info("  - admin (Administrator)");
            log.info("  - teacher1, teacher2 (Teachers)");
            log.info("  - parent1 (Parent)");
            log.info("  - student1, student2, student3 (Students)");
            log.info("📝 5 test consultation records created");
            log.info("");
            log.info("🌐 Swagger UI: http://localhost:8080/swagger-ui.html");
            log.info("🗄️  H2 Console: http://localhost:8080/h2-console (ADMIN account required)");
            log.info("");
        };
    }

    /**
     * 수업 및 스케줄 생성
     * - 레벨테스트 Course 및 스케줄
     * - 일반 영어 수업 Course 및 스케줄
     */
    private void createCoursesAndSchedules() {
        // 레벨테스트 Course 생성
        if (courseRepository.findByCourseName("레벨테스트").isEmpty()) {
            log.info("Creating level test course and schedules...");
            
            Course levelTestCourse = Course.builder()
                    .courseName("레벨테스트")
                    .description("영어 레벨 측정을 위한 테스트")
                    .maxStudents(1) // 1:1 테스트
                    .durationMinutes(60) // 60분
                    .level("ALL")
                    .isActive(true)
                    .color("#FF6B6B")
                    .build();

            courseRepository.save(levelTestCourse);
            log.info("✓ Level test course created: {}", levelTestCourse.getCourseName());
            
            createSchedulesForCourse(levelTestCourse, "레벨테스트 예약 가능");
        }

        // 일반 영어 수업 Course 생성
        if (courseRepository.findByCourseName("영어 수업").isEmpty()) {
            log.info("Creating English class course and schedules...");
            
            Course englishCourse = Course.builder()
                    .courseName("영어 수업")
                    .description("일반 영어 수업")
                    .maxStudents(6) // 최대 6명
                    .durationMinutes(60) // 60분
                    .level("ALL")
                    .isActive(true)
                    .color("#4ECDC4")
                    .build();

            courseRepository.save(englishCourse);
            log.info("✓ English class course created: {}", englishCourse.getCourseName());
            
            createSchedulesForCourse(englishCourse, "영어 수업 예약 가능");
        }
    }

    /**
     * 특정 Course에 대한 시간대별 스케줄 생성
     * 
     * @param course 스케줄을 생성할 Course
     * @param memo 스케줄 메모
     */
    private void createSchedulesForCourse(Course course, String memo) {
        // 향후 30일간 시간대별 스케줄 생성
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);

        // 운영 시간: 09:00 ~ 18:00 (1시간 간격)
        LocalTime[] timeSlots = {
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0)
        };

        int scheduleCount = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 일요일과 공휴일만 제외 (토요일은 모든 수업 가능)
            if (date.getDayOfWeek().getValue() == 7 || holidayService.isHoliday(date)) {
                continue;
            }

            for (LocalTime startTime : timeSlots) {
                LocalTime endTime = startTime.plusMinutes(course.getDurationMinutes());

                CourseSchedule schedule = CourseSchedule.builder()
                        .course(course)
                        .scheduleDate(date)
                        .startTime(startTime)
                        .endTime(endTime)
                        .dayOfWeek(date.getDayOfWeek().name())
                        .currentStudents(0)
                        .isCancelled(false)
                        .memo(memo)
                        .build();

                scheduleRepository.save(schedule);
                scheduleCount++;
            }
        }

        log.info("✓ {} schedules created for course: {}", scheduleCount, course.getCourseName());
    }

    /**
     * 테스트용 출석 데이터 생성
     */
    private void createAttendanceRecords() {
        // 학부모 계정과 연결된 학생 찾기
        User parent1 = userRepository.findByUsername("parent1").orElse(null);
        if (parent1 == null) return;

        List<Student> parentStudents = studentRepository.findByParentPhoneAndIsActive(parent1.getPhoneNumber(), true);
        if (parentStudents.isEmpty()) return;

        // 오늘과 어제 날짜의 스케줄 찾기
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDateBetween(yesterday, today);
        
        for (Student student : parentStudents) {
            for (CourseSchedule schedule : schedules) {
                // 출석 레코드가 없으면 생성 (간단한 체크)
                try {
                    Attendance attendance = Attendance.builder()
                            .student(student)
                            .schedule(schedule)
                            .status(AttendanceStatus.PRESENT)
                            .build();
                    
                    attendanceRepository.save(attendance);
                    log.info("✓ Test attendance created: student={}, date={}", 
                            student.getStudentName(), schedule.getScheduleDate());
                } catch (Exception e) {
                    // 이미 존재하면 무시
                    log.debug("Attendance already exists for student={}, schedule={}", 
                            student.getStudentName(), schedule.getId());
                }
            }
        }
    }
}
