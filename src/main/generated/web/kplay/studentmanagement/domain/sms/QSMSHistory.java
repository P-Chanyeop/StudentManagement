package web.kplay.studentmanagement.domain.sms;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSMSHistory is a Querydsl query type for SMSHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSMSHistory extends EntityPathBase<SMSHistory> {

    private static final long serialVersionUID = 1388434465L;

    public static final QSMSHistory sMSHistory = new QSMSHistory("sMSHistory");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath category = createString("category");

    public final NumberPath<Integer> cost = createNumber("cost", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath errorMessage = createString("errorMessage");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath message = createString("message");

    public final StringPath msgId = createString("msgId");

    public final StringPath receiverName = createString("receiverName");

    public final StringPath receiverNumber = createString("receiverNumber");

    public final StringPath senderNumber = createString("senderNumber");

    public final DateTimePath<java.time.LocalDateTime> sentAt = createDateTime("sentAt", java.time.LocalDateTime.class);

    public final StringPath smsType = createString("smsType");

    public final EnumPath<SMSHistory.SMSStatus> status = createEnum("status", SMSHistory.SMSStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSMSHistory(String variable) {
        super(SMSHistory.class, forVariable(variable));
    }

    public QSMSHistory(Path<? extends SMSHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSMSHistory(PathMetadata metadata) {
        super(SMSHistory.class, metadata);
    }

}

