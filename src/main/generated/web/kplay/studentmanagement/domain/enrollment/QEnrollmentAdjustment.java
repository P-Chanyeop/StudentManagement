package web.kplay.studentmanagement.domain.enrollment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEnrollmentAdjustment is a Querydsl query type for EnrollmentAdjustment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEnrollmentAdjustment extends EntityPathBase<EnrollmentAdjustment> {

    private static final long serialVersionUID = 611115306L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEnrollmentAdjustment enrollmentAdjustment = new QEnrollmentAdjustment("enrollmentAdjustment");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final EnumPath<EnrollmentAdjustment.AdjustmentType> adjustmentType = createEnum("adjustmentType", EnrollmentAdjustment.AdjustmentType.class);

    public final web.kplay.studentmanagement.domain.user.QUser admin;

    public final NumberPath<Integer> countChange = createNumber("countChange", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final web.kplay.studentmanagement.domain.course.QEnrollment enrollment;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath reason = createString("reason");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QEnrollmentAdjustment(String variable) {
        this(EnrollmentAdjustment.class, forVariable(variable), INITS);
    }

    public QEnrollmentAdjustment(Path<? extends EnrollmentAdjustment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEnrollmentAdjustment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEnrollmentAdjustment(PathMetadata metadata, PathInits inits) {
        this(EnrollmentAdjustment.class, metadata, inits);
    }

    public QEnrollmentAdjustment(Class<? extends EnrollmentAdjustment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("admin")) : null;
        this.enrollment = inits.isInitialized("enrollment") ? new web.kplay.studentmanagement.domain.course.QEnrollment(forProperty("enrollment"), inits.get("enrollment")) : null;
    }

}

