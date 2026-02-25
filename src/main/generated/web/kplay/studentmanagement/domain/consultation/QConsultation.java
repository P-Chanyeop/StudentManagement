package web.kplay.studentmanagement.domain.consultation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QConsultation is a Querydsl query type for Consultation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QConsultation extends EntityPathBase<Consultation> {

    private static final long serialVersionUID = -371909123L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConsultation consultation = new QConsultation("consultation");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath attachmentFileUrl = createString("attachmentFileUrl");

    public final web.kplay.studentmanagement.domain.user.QUser consultant;

    public final DatePath<java.time.LocalDate> consultationDate = createDate("consultationDate", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> consultationTime = createTime("consultationTime", java.time.LocalTime.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath recordingFileUrl = createString("recordingFileUrl");

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QConsultation(String variable) {
        this(Consultation.class, forVariable(variable), INITS);
    }

    public QConsultation(Path<? extends Consultation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QConsultation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QConsultation(PathMetadata metadata, PathInits inits) {
        this(Consultation.class, metadata, inits);
    }

    public QConsultation(Class<? extends Consultation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.consultant = inits.isInitialized("consultant") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("consultant")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

