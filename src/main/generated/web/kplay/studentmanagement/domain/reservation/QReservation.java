package web.kplay.studentmanagement.domain.reservation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservation is a Querydsl query type for Reservation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservation extends EntityPathBase<Reservation> {

    private static final long serialVersionUID = -1957575591L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservation reservation = new QReservation("reservation");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final StringPath cancelReason = createString("cancelReason");

    public final StringPath consultationType = createString("consultationType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final web.kplay.studentmanagement.domain.course.QEnrollment enrollment;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final DatePath<java.time.LocalDate> reservationDate = createDate("reservationDate", java.time.LocalDate.class);

    public final StringPath reservationSource = createString("reservationSource");

    public final TimePath<java.time.LocalTime> reservationTime = createTime("reservationTime", java.time.LocalTime.class);

    public final EnumPath<ReservationStatus> status = createEnum("status", ReservationStatus.class);

    public final web.kplay.studentmanagement.domain.student.QStudent student;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QReservation(String variable) {
        this(Reservation.class, forVariable(variable), INITS);
    }

    public QReservation(Path<? extends Reservation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservation(PathMetadata metadata, PathInits inits) {
        this(Reservation.class, metadata, inits);
    }

    public QReservation(Class<? extends Reservation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.enrollment = inits.isInitialized("enrollment") ? new web.kplay.studentmanagement.domain.course.QEnrollment(forProperty("enrollment"), inits.get("enrollment")) : null;
        this.student = inits.isInitialized("student") ? new web.kplay.studentmanagement.domain.student.QStudent(forProperty("student"), inits.get("student")) : null;
    }

}

