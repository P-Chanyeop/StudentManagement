package web.kplay.studentmanagement.domain.teacher;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTeacherAttendance is a Querydsl query type for TeacherAttendance
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTeacherAttendance extends EntityPathBase<TeacherAttendance> {

    private static final long serialVersionUID = 1099057486L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTeacherAttendance teacherAttendance = new QTeacherAttendance("teacherAttendance");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final DatePath<java.time.LocalDate> attendanceDate = createDate("attendanceDate", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> checkInTime = createDateTime("checkInTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> checkOutTime = createDateTime("checkOutTime", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final web.kplay.studentmanagement.domain.user.QUser teacher;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTeacherAttendance(String variable) {
        this(TeacherAttendance.class, forVariable(variable), INITS);
    }

    public QTeacherAttendance(Path<? extends TeacherAttendance> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTeacherAttendance(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTeacherAttendance(PathMetadata metadata, PathInits inits) {
        this(TeacherAttendance.class, metadata, inits);
    }

    public QTeacherAttendance(Class<? extends TeacherAttendance> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.teacher = inits.isInitialized("teacher") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("teacher")) : null;
    }

}

