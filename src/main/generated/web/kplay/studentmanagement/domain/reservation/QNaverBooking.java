package web.kplay.studentmanagement.domain.reservation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNaverBooking is a Querydsl query type for NaverBooking
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNaverBooking extends EntityPathBase<NaverBooking> {

    private static final long serialVersionUID = -1572608452L;

    public static final QNaverBooking naverBooking = new QNaverBooking("naverBooking");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final StringPath bookingNumber = createString("bookingNumber");

    public final StringPath bookingTime = createString("bookingTime");

    public final StringPath cancelDate = createString("cancelDate");

    public final StringPath comment = createString("comment");

    public final StringPath confirmDate = createString("confirmDate");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath deposit = createString("deposit");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath option = createString("option");

    public final StringPath orderDate = createString("orderDate");

    public final StringPath phone = createString("phone");

    public final StringPath product = createString("product");

    public final StringPath quantity = createString("quantity");

    public final StringPath school = createString("school");

    public final StringPath status = createString("status");

    public final StringPath studentName = createString("studentName");

    public final DateTimePath<java.time.LocalDateTime> syncedAt = createDateTime("syncedAt", java.time.LocalDateTime.class);

    public final StringPath totalPrice = createString("totalPrice");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNaverBooking(String variable) {
        super(NaverBooking.class, forVariable(variable));
    }

    public QNaverBooking(Path<? extends NaverBooking> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNaverBooking(PathMetadata metadata) {
        super(NaverBooking.class, metadata);
    }

}

