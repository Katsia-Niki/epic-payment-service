package by.nikiforova.payment.repository;

import by.nikiforova.payment.dto.PaymentSum;
import by.nikiforova.payment.entity.Payment;
import by.nikiforova.payment.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(Long userId);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByStatus(PaymentStatus status);

    @Aggregation(pipeline = {
            "{ $match: { user_id: ?0, timestamp: { $gte: ?1, $lte: ?2 }, status: 'SUCCESS'} }",
            "{ $group: { _id: null, total: { $sum: '$payment_amount' } } }"
    })
    PaymentSum sumByUserIdAndTimestampBetween(Long userId, LocalDateTime from, LocalDateTime to);

    @Aggregation(pipeline = {
            "{ $match: { timestamp: { $gte: ?0, $lte: ?1 }, status: 'SUCCESS' } }",
            "{ $group: { _id: null, total: { $sum: '$payment_amount' } } }"
    })
    PaymentSum sumByTimestampBetween(LocalDateTime from, LocalDateTime to);

}

