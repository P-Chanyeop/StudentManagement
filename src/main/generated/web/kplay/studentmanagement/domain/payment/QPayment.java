package web.kplay.studentmanagement.domain.payment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPayment is a Querydsl query type for Payment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPayment extends EntityPathBase<Payment> {

    private static final long serialVersionUID = 433919181L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPayment payment = new QPayment("payment");

    public final web.kplay.studentmanagement.domain.QBaseEntity _super = new web.kplay.studentmanagement.domain.QBaseEntity(this);

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final StringPath buyerEmail = createString("buyerEmail");

    public final StringPath buyerName = createString("buyerName");

    public final StringPath buyerTel = createString("buyerTel");

    public final StringPath cardName = createString("cardName");

    public final StringPath cardNumber = createString("cardNumber");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath failedReason = createString("failedReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath impUid = createString("impUid");

    public final web.kplay.studentmanagement.domain.invoice.QInvoice invoice;

    public final StringPath memo = createString("memo");

    public final StringPath merchantUid = createString("merchantUid");

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath paymentMethod = createString("paymentMethod");

    public final StringPath pgProvider = createString("pgProvider");

    public final StringPath receiptUrl = createString("receiptUrl");

    public final EnumPath<PaymentStatus> status = createEnum("status", PaymentStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final DateTimePath<java.time.LocalDateTime> vbankDate = createDateTime("vbankDate", java.time.LocalDateTime.class);

    public final StringPath vbankHolder = createString("vbankHolder");

    public final StringPath vbankName = createString("vbankName");

    public final StringPath vbankNum = createString("vbankNum");

    public QPayment(String variable) {
        this(Payment.class, forVariable(variable), INITS);
    }

    public QPayment(Path<? extends Payment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPayment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPayment(PathMetadata metadata, PathInits inits) {
        this(Payment.class, metadata, inits);
    }

    public QPayment(Class<? extends Payment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.invoice = inits.isInitialized("invoice") ? new web.kplay.studentmanagement.domain.invoice.QInvoice(forProperty("invoice"), inits.get("invoice")) : null;
    }

}

