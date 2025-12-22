package web.kplay.studentmanagement.domain.course;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEnrollment is a Querydsl query type for Enrollment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEnrollment extends EntityPathBase<Enrollment> {

    private static final long serialVersionUID = -1949309466L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEnrollment enrollment = new QEnrollment("enrollment");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final QCourse course;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath memo = createString("memo");

    public final NumberPath<Integer> remainingCount = createNumber("remainingCount", Integer.class);

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    public final NumberPath<Integer> totalCount = createNumber("totalCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> usedCount = createNumber("usedCount", Integer.class);

    public QEnrollment(String variable) {
        this(Enrollment.class, forVariable(variable), INITS);
    }

    public QEnrollment(Path<? extends Enrollment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEnrollment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEnrollment(PathMetadata metadata, PathInits inits) {
        this(Enrollment.class, metadata, inits);
    }

    public QEnrollment(Class<? extends Enrollment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.course = inits.isInitialized("course") ? new QCourse(forProperty("course"), inits.get("course")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

