package web.kplay.studentmanagement.domain.student;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudent is a Querydsl query type for Student
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudent extends EntityPathBase<Student> {

    private static final long serialVersionUID = -1434669001L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudent student = new QStudent("student");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath address = createString("address");

    public final BooleanPath assignedGrammar = createBoolean("assignedGrammar");

    public final BooleanPath assignedPhonics = createBoolean("assignedPhonics");

    public final BooleanPath assignedSightword = createBoolean("assignedSightword");

    public final BooleanPath assignedVocabulary = createBoolean("assignedVocabulary");

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final web.kplay.studentmanagement.domain.course.QCourse defaultCourse;

    public final StringPath englishLevel = createString("englishLevel");

    public final ListPath<web.kplay.studentmanagement.domain.course.Enrollment, web.kplay.studentmanagement.domain.course.QEnrollment> enrollments = this.<web.kplay.studentmanagement.domain.course.Enrollment, web.kplay.studentmanagement.domain.course.QEnrollment>createList("enrollments", web.kplay.studentmanagement.domain.course.Enrollment.class, web.kplay.studentmanagement.domain.course.QEnrollment.class, PathInits.DIRECT2);

    public final StringPath gender = createString("gender");

    public final StringPath grade = createString("grade");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath memo = createString("memo");

    public final StringPath parentEmail = createString("parentEmail");

    public final StringPath parentName = createString("parentName");

    public final StringPath parentPhone = createString("parentPhone");

    public final web.kplay.studentmanagement.domain.user.QUser parentUser;

    public final StringPath school = createString("school");

    public final StringPath studentName = createString("studentName");

    public final StringPath studentPhone = createString("studentPhone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QStudent(String variable) {
        this(Student.class, forVariable(variable), INITS);
    }

    public QStudent(Path<? extends Student> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudent(PathMetadata metadata, PathInits inits) {
        this(Student.class, metadata, inits);
    }

    public QStudent(Class<? extends Student> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.defaultCourse = inits.isInitialized("defaultCourse") ? new web.kplay.studentmanagement.domain.course.QCourse(forProperty("defaultCourse"), inits.get("defaultCourse")) : null;
        this.parentUser = inits.isInitialized("parentUser") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("parentUser")) : null;
    }

}

