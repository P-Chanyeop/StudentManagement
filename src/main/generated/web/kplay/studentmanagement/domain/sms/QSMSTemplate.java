package web.kplay.studentmanagement.domain.sms;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSMSTemplate is a Querydsl query type for SMSTemplate
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSMSTemplate extends EntityPathBase<SMSTemplate> {

    private static final long serialVersionUID = 100060813L;

    public static final QSMSTemplate sMSTemplate = new QSMSTemplate("sMSTemplate");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath category = createString("category");

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath name = createString("name");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSMSTemplate(String variable) {
        super(SMSTemplate.class, forVariable(variable));
    }

    public QSMSTemplate(Path<? extends SMSTemplate> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSMSTemplate(PathMetadata metadata) {
        super(SMSTemplate.class, metadata);
    }

}

