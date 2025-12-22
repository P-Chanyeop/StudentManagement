package web.kplay.studentmanagement.domain.course;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCourseSchedule is a Querydsl query type for CourseSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCourseSchedule extends EntityPathBase<CourseSchedule> {

    private static final long serialVersionUID = 1394593204L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCourseSchedule courseSchedule = new QCourseSchedule("courseSchedule");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath cancelReason = createString("cancelReason");

    public final QCourse course;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> currentStudents = createNumber("currentStudents", Integer.class);

    public final StringPath dayOfWeek = createString("dayOfWeek");

    public final TimePath<java.time.LocalTime> endTime = createTime("endTime", java.time.LocalTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isCancelled = createBoolean("isCancelled");

    public final StringPath memo = createString("memo");

    public final DatePath<java.time.LocalDate> scheduleDate = createDate("scheduleDate", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> startTime = createTime("startTime", java.time.LocalTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCourseSchedule(String variable) {
        this(CourseSchedule.class, forVariable(variable), INITS);
    }

    public QCourseSchedule(Path<? extends CourseSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCourseSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCourseSchedule(PathMetadata metadata, PathInits inits) {
        this(CourseSchedule.class, metadata, inits);
    }

    public QCourseSchedule(Class<? extends CourseSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.course = inits.isInitialized("course") ? new QCourse(forProperty("course"), inits.get("course")) : null;
    }

}

