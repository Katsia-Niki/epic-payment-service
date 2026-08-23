package by.nikiforova.payment.service;

import by.nikiforova.payment.entity.Payment;
import by.nikiforova.payment.entity.PaymentStatus;
import by.nikiforova.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(Long userId, Long orderId, BigDecimal paymentAmount) {
        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(paymentAmount)
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .status(PaymentStatus.SUCCESS)
                .build();

        return paymentRepository.save(payment);
    }

    public List<Payment> findPayments(Long userId, Long orderId, PaymentStatus status) {
        int criteriaCount = 0;
        if (userId != null) {
            criteriaCount++;
        }
        if (orderId != null) {
            criteriaCount++;
        }
        if (status != null) {
            criteriaCount++;
        }
        if (criteriaCount != 1) {
            throw new IllegalArgumentException("Provide exactly one filter: userId, orderId or status");
        }
        if (userId != null) {
            return paymentRepository.findByUserId(userId);
        }
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId);
        }
        return paymentRepository.findByStatus(status);
    }

    public BigDecimal getTotalSumForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetween(userId, from, to);

        BigDecimal sum = BigDecimal.ZERO;
        for (Payment payment : payments) {
            sum = sum.add(payment.getPaymentAmount());
        }

        return sum;
    }

    public BigDecimal getTotalSumForAllUsers(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository.findByTimestampBetween(from, to);

        BigDecimal sum = BigDecimal.ZERO;
        for (Payment payment : payments) {
            sum = sum.add(payment.getPaymentAmount());
        }

        return sum;
    }

}
