package web.kplay.studentmanagement.domain.leveltest;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLevelTest is a Querydsl query type for LevelTest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLevelTest extends EntityPathBase<LevelTest> {

    private static final long serialVersionUID = 688926605L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLevelTest levelTest = new QLevelTest("levelTest");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath feedback = createString("feedback");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath improvements = createString("improvements");

    public final StringPath memo = createString("memo");

    public final BooleanPath messageNotificationSent = createBoolean("messageNotificationSent");

    public final StringPath recommendedLevel = createString("recommendedLevel");

    public final StringPath strengths = createString("strengths");

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    public final web.kplay.studentmanagement.domain.user.QUser teacher;

    public final DatePath<java.time.LocalDate> testDate = createDate("testDate", java.time.LocalDate.class);

    public final StringPath testResult = createString("testResult");

    public final NumberPath<Integer> testScore = createNumber("testScore", Integer.class);

    public final StringPath testStatus = createString("testStatus");

    public final TimePath<java.time.LocalTime> testTime = createTime("testTime", java.time.LocalTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QLevelTest(String variable) {
        this(LevelTest.class, forVariable(variable), INITS);
    }

    public QLevelTest(Path<? extends LevelTest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLevelTest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLevelTest(PathMetadata metadata, PathInits inits) {
        this(LevelTest.class, metadata, inits);
    }

    public QLevelTest(Class<? extends LevelTest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
        this.teacher = inits.isInitialized("teacher") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("teacher")) : null;
    }

}

