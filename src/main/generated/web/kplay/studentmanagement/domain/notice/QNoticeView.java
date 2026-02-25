package web.kplay.studentmanagement.domain.notice;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNoticeView is a Querydsl query type for NoticeView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNoticeView extends EntityPathBase<NoticeView> {

    private static final long serialVersionUID = 2108833186L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNoticeView noticeView = new QNoticeView("noticeView");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QNotice notice;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final web.kplay.studentmanagement.domain.user.QUser user;

    public QNoticeView(String variable) {
        this(NoticeView.class, forVariable(variable), INITS);
    }

    public QNoticeView(Path<? extends NoticeView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNoticeView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNoticeView(PathMetadata metadata, PathInits inits) {
        this(NoticeView.class, metadata, inits);
    }

    public QNoticeView(Class<? extends NoticeView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.notice = inits.isInitialized("notice") ? new QNotice(forProperty("notice"), inits.get("notice")) : null;
        this.user = inits.isInitialized("user") ? new web.kplay.studentmanagement.domain.user.QUser(forProperty("user")) : null;
    }

}

