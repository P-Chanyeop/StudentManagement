package web.kplay.studentmanagement.domain.parent;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QParentStudentRelation is a Querydsl query type for ParentStudentRelation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParentStudentRelation extends EntityPathBase<ParentStudentRelation> {

    private static final long serialVersionUID = -1648526726L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QParentStudentRelation parentStudentRelation = new QParentStudentRelation("parentStudentRelation");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final BooleanPath canMakeReservations = createBoolean("canMakeReservations");

    public final BooleanPath canReceiveMessages = createBoolean("canReceiveMessages");

    public final BooleanPath canViewAttendance = createBoolean("canViewAttendance");

    public final BooleanPath canViewGrades = createBoolean("canViewGrades");

    public final BooleanPath canViewInvoices = createBoolean("canViewInvoices");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final web.kplay.studentmanagement.domain.user.QUser parent;

    public final StringPath relationship = createString("relationship");

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QParentStudentRelation(String variable) {
        this(ParentStudentRelation.class, forVariable(variable), INITS);
    }

    public QParentStudentRelation(Path<? extends ParentStudentRelation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QParentStudentRelation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QParentStudentRelation(PathMetadata metadata, PathInits inits) {
        this(ParentStudentRelation.class, metadata, inits);
    }

    public QParentStudentRelation(Class<? extends ParentStudentRelation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.parent = inits.isInitialized("parent") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("parent")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

