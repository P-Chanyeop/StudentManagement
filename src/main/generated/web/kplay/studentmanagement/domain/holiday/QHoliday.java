package web.kplay.studentmanagement.domain.holiday;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QHoliday is a Querydsl query type for Holiday
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHoliday extends EntityPathBase<Holiday> {

    private static final long serialVersionUID = -1033597071L;

    public static final QHoliday holiday = new QHoliday("holiday");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> date = createDate("date", java.time.LocalDate.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isRecurring = createBoolean("isRecurring");

    public final StringPath name = createString("name");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QHoliday(String variable) {
        super(Holiday.class, forVariable(variable));
    }

    public QHoliday(Path<? extends Holiday> path) {
        super(path.getType(), path.getMetadata());
    }

    public QHoliday(PathMetadata metadata) {
        super(Holiday.class, metadata);
    }

}

