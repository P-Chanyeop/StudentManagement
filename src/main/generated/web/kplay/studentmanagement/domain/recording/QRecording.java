package web.kplay.studentmanagement.domain.recording;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecording is a Querydsl query type for Recording
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecording extends EntityPathBase<Recording> {

    private static final long serialVersionUID = 1834280291L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecording recording = new QRecording("recording");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath fileName = createString("fileName");

    public final StringPath fileUrl = createString("fileUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final NumberPath<Integer> sessionNumber = createNumber("sessionNumber", Integer.class);

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRecording(String variable) {
        this(Recording.class, forVariable(variable), INITS);
    }

    public QRecording(Path<? extends Recording> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecording(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecording(PathMetadata metadata, PathInits inits) {
        this(Recording.class, metadata, inits);
    }

    public QRecording(Class<? extends Recording> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

