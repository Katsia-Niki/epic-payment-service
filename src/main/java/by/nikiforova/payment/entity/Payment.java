package by.nikiforova.payment.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("order_id")
    private Long orderId;

    @Field("status")
    private PaymentStatus status;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field(name = "payment_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
