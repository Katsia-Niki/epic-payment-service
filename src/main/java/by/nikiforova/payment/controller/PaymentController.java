package by.nikiforova.payment.controller;

import by.nikiforova.payment.dto.request.PaymentRequestDto;
import by.nikiforova.payment.dto.response.PaymentResponseDto;
import by.nikiforova.payment.entity.PaymentStatus;
import by.nikiforova.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(paymentRequestDto));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(@RequestParam (required = false) Long userId,
                                                                   @RequestParam (required = false) Long orderId,
                                                                   @RequestParam (required = false) PaymentStatus status) {

        return ResponseEntity.ok(paymentService.findPayments(userId, orderId, status));
    }

    @GetMapping("/total/user/{userId}")
    public ResponseEntity<BigDecimal> getTotalSumForUser(@PathVariable Long userId,
                                                  @RequestParam LocalDateTime from,
                                                  @RequestParam LocalDateTime to) {

        return ResponseEntity.ok(paymentService.getTotalSumForUser(userId, from, to));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalSum(@RequestParam LocalDateTime from,
                                                  @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getTotalSumForAllUsers(from, to));
    }

}
