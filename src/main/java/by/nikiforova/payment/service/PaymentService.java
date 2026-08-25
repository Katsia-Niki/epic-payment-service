package by.nikiforova.payment.service;

import by.nikiforova.payment.client.RandomNumberClient;
import by.nikiforova.payment.dto.request.PaymentRequestDto;
import by.nikiforova.payment.dto.response.PaymentResponseDto;
import by.nikiforova.payment.entity.Payment;
import by.nikiforova.payment.entity.PaymentStatus;
import by.nikiforova.payment.kafka.PaymentKafkaProducer;
import by.nikiforova.payment.mapper.PaymentMapper;
import by.nikiforova.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentKafkaProducer paymentKafkaProducer;

    public PaymentResponseDto createPayment(PaymentRequestDto  paymentRequestDto) {
        int number = randomNumberClient.getRandomNumber();
        PaymentStatus status = (number % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        Payment payment = paymentMapper.toEntity(paymentRequestDto);
        payment.setTimestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")));
        payment.setStatus(status);

        Payment savedPayment = paymentRepository.save(payment);

        paymentKafkaProducer.sendCreatePaymentEvent(savedPayment);

        return paymentMapper.toResponseDto(savedPayment);
    }

    public List<PaymentResponseDto> findPayments(Long userId, Long orderId, PaymentStatus status) {
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
            return paymentRepository.findByUserId(userId).stream().map(paymentMapper::toResponseDto).toList();
        }
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId).stream().map(paymentMapper::toResponseDto).toList();
        }
        return paymentRepository.findByStatus(status).stream().map(paymentMapper::toResponseDto).toList();
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
