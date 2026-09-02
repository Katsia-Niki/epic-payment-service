package by.nikiforova.payment.dto.response;

import by.nikiforova.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDto(String id,
                                 Long userId,
                                 Long orderId,
                                 PaymentStatus status,
                                 Boolean isPaymentSuccessful,
                                 LocalDateTime timestamp,
                                 BigDecimal paymentAmount) {
}
