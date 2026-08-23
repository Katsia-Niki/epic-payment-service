package by.nikiforova.payment.repository;

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
    List<Payment> findByUserIdAndTimestampBetween(Long userId, LocalDateTime from, LocalDateTime to);
    List<Payment> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

}

