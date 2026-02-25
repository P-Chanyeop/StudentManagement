package web.kplay.studentmanagement.domain.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserMenuSetting is a Querydsl query type for UserMenuSetting
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserMenuSetting extends EntityPathBase<UserMenuSetting> {

    private static final long serialVersionUID = 329358708L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserMenuSetting userMenuSetting = new QUserMenuSetting("userMenuSetting");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath menuOrder = createString("menuOrder");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QUser user;

    public QUserMenuSetting(String variable) {
        this(UserMenuSetting.class, forVariable(variable), INITS);
    }

    public QUserMenuSetting(Path<? extends UserMenuSetting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserMenuSetting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserMenuSetting(PathMetadata metadata, PathInits inits) {
        this(UserMenuSetting.class, metadata, inits);
    }

    public QUserMenuSetting(Class<? extends UserMenuSetting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

