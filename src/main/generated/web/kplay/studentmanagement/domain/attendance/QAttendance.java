package web.kplay.studentmanagement.domain.attendance;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAttendance is a Querydsl query type for Attendance
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAttendance extends EntityPathBase<Attendance> {

    private static final long serialVersionUID = -188410179L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAttendance attendance = new QAttendance("attendance");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final TimePath<java.time.LocalTime> additionalClassEndTime = createTime("additionalClassEndTime", java.time.LocalTime.class);

    public final DatePath<java.time.LocalDate> attendanceDate = createDate("attendanceDate", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> attendanceTime = createTime("attendanceTime", java.time.LocalTime.class);

    public final DateTimePath<java.time.LocalDateTime> checkInTime = createDateTime("checkInTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> checkOutTime = createDateTime("checkOutTime", java.time.LocalDateTime.class);

    public final BooleanPath classCompleted = createBoolean("classCompleted");

    public final web.kplay.studentmanagement.domain.course.QCourse course;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath dcCheck = createString("dcCheck");

    public final NumberPath<Integer> durationMinutes = createNumber("durationMinutes", Integer.class);

    public final TimePath<java.time.LocalTime> expectedLeaveTime = createTime("expectedLeaveTime", java.time.LocalTime.class);

    public final BooleanPath grammarClass = createBoolean("grammarClass");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath lateNotificationSent = createBoolean("lateNotificationSent");

    public final StringPath memo = createString("memo");

    public final web.kplay.studentmanagement.domain.reservation.QNaverBooking naverBooking;

    public final TimePath<java.time.LocalTime> originalExpectedLeaveTime = createTime("originalExpectedLeaveTime", java.time.LocalTime.class);

    public final BooleanPath phonicsClass = createBoolean("phonicsClass");

    public final StringPath reason = createString("reason");

    public final BooleanPath speakingClass = createBoolean("speakingClass");

    public final EnumPath<AttendanceStatus> status = createEnum("status", AttendanceStatus.class);

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final BooleanPath vocabularyClass = createBoolean("vocabularyClass");

    public final StringPath wrCheck = createString("wrCheck");

    public QAttendance(String variable) {
        this(Attendance.class, forVariable(variable), INITS);
    }

    public QAttendance(Path<? extends Attendance> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAttendance(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAttendance(PathMetadata metadata, PathInits inits) {
        this(Attendance.class, metadata, inits);
    }

    public QAttendance(Class<? extends Attendance> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.course = inits.isInitialized("course") ? new web.kplay.studentmanagement.domain.course.QCourse(forProperty("course"), inits.get("course")) : null;
        this.naverBooking = inits.isInitialized("naverBooking") ? new web.kplay.studentmanagement.domain.reservation.QNaverBooking(forProperty("naverBooking")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

