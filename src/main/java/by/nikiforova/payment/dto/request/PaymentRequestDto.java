package by.nikiforova.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequestDto(@NotNull Long userId,
                                @NotNull Long orderId,
                                @Positive @NotNull BigDecimal paymentAmount) {
}
