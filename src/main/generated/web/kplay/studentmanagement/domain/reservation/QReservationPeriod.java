package web.kplay.studentmanagement.domain.reservation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReservationPeriod is a Querydsl query type for ReservationPeriod
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationPeriod extends EntityPathBase<ReservationPeriod> {

    private static final long serialVersionUID = 1111605114L;

    public static final QReservationPeriod reservationPeriod = new QReservationPeriod("reservationPeriod");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> closeTime = createDateTime("closeTime", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> openTime = createDateTime("openTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> reservationEndDate = createDateTime("reservationEndDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> reservationStartDate = createDateTime("reservationStartDate", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QReservationPeriod(String variable) {
        super(ReservationPeriod.class, forVariable(variable));
    }

    public QReservationPeriod(Path<? extends ReservationPeriod> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReservationPeriod(PathMetadata metadata) {
        super(ReservationPeriod.class, metadata);
    }

}

