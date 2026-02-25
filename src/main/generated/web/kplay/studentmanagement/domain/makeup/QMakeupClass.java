package web.kplay.studentmanagement.domain.makeup;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMakeupClass is a Querydsl query type for MakeupClass
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMakeupClass extends EntityPathBase<MakeupClass> {

    private static final long serialVersionUID = -646317253L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMakeupClass makeupClass = new QMakeupClass("makeupClass");

    public final web.kplay.studentmanagement.domain.course.QCourse course;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DatePath<java.time.LocalDate> makeupDate = createDate("makeupDate", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> makeupTime = createTime("makeupTime", java.time.LocalTime.class);

    public final StringPath memo = createString("memo");

    public final DatePath<java.time.LocalDate> originalDate = createDate("originalDate", java.time.LocalDate.class);

    public final StringPath reason = createString("reason");

    public final EnumPath<MakeupStatus> status = createEnum("status", MakeupStatus.class);

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    public QMakeupClass(String variable) {
        this(MakeupClass.class, forVariable(variable), INITS);
    }

    public QMakeupClass(Path<? extends MakeupClass> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMakeupClass(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMakeupClass(PathMetadata metadata, PathInits inits) {
        this(MakeupClass.class, metadata, inits);
    }

    public QMakeupClass(Class<? extends MakeupClass> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.course = inits.isInitialized("course") ? new web.kplay.studentmanagement.domain.course.QCourse(forProperty("course"), inits.get("course")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

