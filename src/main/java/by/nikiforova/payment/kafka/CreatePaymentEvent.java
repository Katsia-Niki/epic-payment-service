package by.nikiforova.payment.kafka;

import by.nikiforova.payment.entity.PaymentStatus;

public record CreatePaymentEvent(String paymentId,
                                 Long orderId,
                                 PaymentStatus status) {
}
