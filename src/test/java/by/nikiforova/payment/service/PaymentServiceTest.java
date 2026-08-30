package by.nikiforova.payment.service;

import by.nikiforova.payment.client.RandomNumberClient;
import by.nikiforova.payment.dto.PaymentSum;
import by.nikiforova.payment.dto.request.PaymentRequestDto;
import by.nikiforova.payment.dto.response.PaymentResponseDto;
import by.nikiforova.payment.entity.Payment;
import by.nikiforova.payment.entity.PaymentStatus;
import by.nikiforova.payment.kafka.PaymentKafkaProducer;
import by.nikiforova.payment.mapper.PaymentMapper;
import by.nikiforova.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient  randomNumberClient;

    @Mock
    private PaymentKafkaProducer paymentKafkaProducer;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestDto paymentRequestDto;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("Ivan", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        paymentRequestDto = new PaymentRequestDto(1L, 50L, new BigDecimal(75));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createPayment, even number - success")
    void createPaymentWhenRandomEvenShouldSaveSuccessAndSendKafka() {
        when(randomNumberClient.getRandomNumber()).thenReturn(2);

        Payment payment = Payment.builder()
                .userId(1L)
                .orderId(50L)
                .paymentAmount(new BigDecimal(75))
                .build();

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);

        Payment saved = Payment.builder()
                .id("newId1")
                .userId(1L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 1L, 50L, PaymentStatus.SUCCESS,
                saved.getTimestamp(), new BigDecimal(75));

        when(paymentMapper.toResponseDto(saved)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(paymentRequestDto);

        assertEquals(responseDto, result);
        assertEquals(PaymentStatus.SUCCESS, result.status());

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentKafkaProducer).sendCreatePaymentEvent(saved);
    }

    @Test
    @DisplayName("createPayment, odd number - failed")
    void createPaymentWhenRandomOddShouldFail() {
        when(randomNumberClient.getRandomNumber()).thenReturn(3);

        Payment payment = Payment.builder()
                .userId(1L)
                .orderId(50L)
                .paymentAmount(new BigDecimal(75))
                .build();

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);

        Payment saved = Payment.builder()
                .id("newId1")
                .userId(1L)
                .orderId(50L)
                .status(PaymentStatus.FAILED)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 1L, 50L, PaymentStatus.FAILED,
                saved.getTimestamp(), new BigDecimal(75));

        when(paymentMapper.toResponseDto(saved)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(paymentRequestDto);

        assertEquals(responseDto, result);
        assertEquals(PaymentStatus.FAILED, result.status());

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentKafkaProducer).sendCreatePaymentEvent(saved);
    }

    @Test
    @DisplayName("createPayment - AccessDeniedException")
    void createPaymentWhenOtherUserShouldThrowAccessDeniedException() {
        PaymentRequestDto otherUserRequest = new PaymentRequestDto(2L, 50L, new BigDecimal(75));

        assertThrows(AccessDeniedException.class,
                () -> paymentService.createPayment(otherUserRequest));

        verify(randomNumberClient, never()).getRandomNumber();
        verify(paymentRepository, never()).save(any());
        verify(paymentKafkaProducer, never()).sendCreatePaymentEvent(any());
    }

    @Test
    @DisplayName("createPayment - admin can create for other user")
    void createPaymentWhenAdminAndOtherUserShouldSave() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("Admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        PaymentRequestDto otherUserRequest = new PaymentRequestDto(2L, 50L, new BigDecimal(75));

        when(randomNumberClient.getRandomNumber()).thenReturn(2);

        Payment payment = Payment.builder()
                .userId(2L)
                .orderId(50L)
                .paymentAmount(new BigDecimal(75))
                .build();
        when(paymentMapper.toEntity(otherUserRequest)).thenReturn(payment);

        Payment saved = Payment.builder()
                .id("newId1")
                .userId(2L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 2L, 50L, PaymentStatus.SUCCESS,
                saved.getTimestamp(), new BigDecimal(75));
        when(paymentMapper.toResponseDto(saved)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(otherUserRequest);

        assertEquals(responseDto, result);

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentKafkaProducer).sendCreatePaymentEvent(saved);
    }

    @Test
    @DisplayName("findPayments, by userId - success")
    void findPaymentsWhenOwnUserIdShouldReturnPayments() {
        Payment payment = Payment.builder()
                .id("newId1")
                .userId(1L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();
        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(payment));

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 1L, 50L, PaymentStatus.SUCCESS,
                payment.getTimestamp(), new BigDecimal(75));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        List<PaymentResponseDto> result = paymentService.findPayments(1L, null, null);

        assertEquals(List.of(responseDto), result);

        verify(paymentRepository).findByUserId(1L);
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments, by orderId - success")
    void findPaymentsByOrderIdWhenOwnUserIdShouldReturnPayments() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("Admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Payment payment = Payment.builder()
                .id("newId1")
                .userId(1L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();
        when(paymentRepository.findByOrderId(50L)).thenReturn(List.of(payment));

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 1L, 50L, PaymentStatus.SUCCESS,
                payment.getTimestamp(), new BigDecimal(75));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        List<PaymentResponseDto> result = paymentService.findPayments(null, 50L, null);

        assertEquals(List.of(responseDto), result);

        verify(paymentRepository).findByOrderId(50L);
        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments, by Status - success")
    void findPaymentsByStatusWhenOwnUserIdShouldReturnPayments() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("Admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Payment payment = Payment.builder()
                .id("newId1")
                .userId(1L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS)).thenReturn(List.of(payment));

        PaymentResponseDto responseDto = new PaymentResponseDto("newId1", 1L, 50L, PaymentStatus.SUCCESS,
                payment.getTimestamp(), new BigDecimal(75));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        List<PaymentResponseDto> result = paymentService.findPayments(null, null, PaymentStatus.SUCCESS);

        assertEquals(List.of(responseDto), result);

        verify(paymentRepository).findByStatus(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("findPayments - IllegalArgumentException")
    void findPaymentsWhenTwoFiltersShouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.findPayments(1L, 50L, null));

        assertEquals("Provide exactly one filter: userId, orderId or status", ex.getMessage());

        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments, zero filters - IllegalArgumentException")
    void findPaymentsWhenZeroFiltersShouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.findPayments(null, null, null));

        assertEquals("Provide exactly one filter: userId, orderId or status", ex.getMessage());

        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments - AccessDeniedException")
    void findPaymentsWhenOtherUserShouldThrowAccessDeniedException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.findPayments(2L, null, null));

        assertEquals("Access denied", ex.getMessage());

        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments, by orderId - AccessDeniedException")
    void findPaymentsByOrderShouldThrowAccessDeniedException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.findPayments(null, 50L, null));

        assertEquals("Access denied", ex.getMessage());

        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments, by status - AccessDeniedException")
    void findPaymentsByStatusShouldThrowAccessDeniedException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.findPayments(null, null, PaymentStatus.SUCCESS));

        assertEquals("Access denied", ex.getMessage());

        verify(paymentRepository, never()).findByUserId(any());
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("findPayments - admin can get for other user")
    void findPaymentsWhenAdminCanGetForOtherUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("Admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Payment payment = Payment.builder()
                .id("newId2")
                .userId(2L)
                .orderId(50L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal(75))
                .timestamp(LocalDateTime.now(ZoneId.of("Europe/Minsk")))
                .build();
        when(paymentRepository.findByUserId(2L)).thenReturn(List.of(payment));

        PaymentResponseDto responseDto = new PaymentResponseDto("newId2", 2L, 50L, PaymentStatus.SUCCESS,
                payment.getTimestamp(), new BigDecimal(75));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        List<PaymentResponseDto> result = paymentService.findPayments(2L, null, null);

        assertEquals(List.of(responseDto), result);

        verify(paymentRepository).findByUserId(2L);
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("getTotalSumForUser - sum of two payments")
    void getTotalSumForUserWhenTwoPaymentsShouldReturnSum() {
        LocalDateTime from = LocalDateTime.of(2026, Month.MARCH, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, Month.MARCH, 31, 23, 59);

        when(paymentRepository.sumByUserIdAndTimestampBetween(1L, from, to))
                .thenReturn(new PaymentSum(new BigDecimal("75.50")));

        BigDecimal result = paymentService.getTotalSumForUser(1L, from, to);

        assertEquals(new BigDecimal("75.50"), result);

        verify(paymentRepository).sumByUserIdAndTimestampBetween(1L, from, to);
    }

    @Test
    @DisplayName("getTotalSumForUser - AccessDeniedException")
    void getTotalSumForUserWhenOtherUserShouldThrowAccessDeniedException() {
        LocalDateTime from = LocalDateTime.of(2026, Month.MARCH, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, Month.MARCH, 31, 23, 59);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.getTotalSumForUser(2L, from, to));

        assertEquals("Access denied", ex.getMessage());

        verify(paymentRepository, never()).sumByUserIdAndTimestampBetween(any(), any(), any());
    }

    @Test
    @DisplayName("getTotalSum - success")
    void getTotalSumForAllUsersWhenTwoPaymentsShouldReturnSum() {
        LocalDateTime from = LocalDateTime.of(2026, Month.MARCH, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, Month.MARCH, 31, 23, 59);

        when(paymentRepository.sumByTimestampBetween(from, to))
                .thenReturn(new PaymentSum(new BigDecimal("75.50")));

        BigDecimal result = paymentService.getTotalSumForAllUsers(from, to);

        assertEquals(new BigDecimal("75.50"), result);

        verify(paymentRepository).sumByTimestampBetween(from, to);
    }
}
