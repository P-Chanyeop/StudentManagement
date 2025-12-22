package web.kplay.studentmanagement.domain.sms;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSMSConfig is a Querydsl query type for SMSConfig
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSMSConfig extends EntityPathBase<SMSConfig> {

    private static final long serialVersionUID = -785715691L;

    public static final QSMSConfig sMSConfig = new QSMSConfig("sMSConfig");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath apiKey = createString("apiKey");

    public final BooleanPath autoAttendanceReminder = createBoolean("autoAttendanceReminder");

    public final BooleanPath autoEnrollmentExpiry = createBoolean("autoEnrollmentExpiry");

    public final BooleanPath autoPaymentReminder = createBoolean("autoPaymentReminder");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final EnumPath<SMSConfig.SMSProvider> provider = createEnum("provider", SMSConfig.SMSProvider.class);

    public final StringPath senderNumber = createString("senderNumber");

    public final BooleanPath testMode = createBoolean("testMode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath userId = createString("userId");

    public QSMSConfig(String variable) {
        super(SMSConfig.class, forVariable(variable));
    }

    public QSMSConfig(Path<? extends SMSConfig> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSMSConfig(PathMetadata metadata) {
        super(SMSConfig.class, metadata);
    }

}

