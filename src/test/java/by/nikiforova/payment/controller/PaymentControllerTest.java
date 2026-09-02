package by.nikiforova.payment.controller;

import by.nikiforova.payment.dto.request.PaymentRequestDto;
import by.nikiforova.payment.entity.Payment;
import by.nikiforova.payment.entity.PaymentStatus;
import by.nikiforova.payment.kafka.PaymentKafkaProducer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private PaymentKafkaProducer paymentKafkaProducer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        WIRE_MOCK.resetAll();
    }

    @Test
    @DisplayName("POST /api/payments - SUCCESS")
    void createPaymentWhenRandomEvenShouldSaveSuccess() throws Exception {
        stubRandomNumber("2");
        PaymentRequestDto request = new PaymentRequestDto(1L, 50L, new BigDecimal("75.00"));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + userToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.orderId").value(50))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.isPaymentSuccessful").value(true))
                .andExpect(jsonPath("$.paymentAmount").value(75.00))
                .andExpect(jsonPath("$.timestamp").exists());

        List<Payment> saved = paymentRepository.findAll();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.getFirst().getIsPaymentSuccessful()).isEqualTo(Boolean.TRUE);
        assertThat(saved.getFirst().getUserId()).isEqualTo(1L);

        WIRE_MOCK.verify(1, getRequestedFor(urlPathEqualTo("/integers")));
    }

    private void stubRandomNumber(String number) {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/integers")).willReturn(ok(number)));
    }

    private String userToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("Ivan")
                .claim("userId", userId)
                .claim("role", "USER")
                .signWith(key)
                .compact();
    }
}
