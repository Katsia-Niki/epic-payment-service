package by.nikiforova.payment.kafka;

import by.nikiforova.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, CreatePaymentEvent> kafkaTemplate;

    @Value("${kafka.topic.create-payment}")
    private String topic;

    public void sendCreatePaymentEvent(Payment payment) {

        CreatePaymentEvent createPaymentEvent = new CreatePaymentEvent(payment.getId(),
                payment.getOrderId(), payment.getStatus());
        kafkaTemplate.send(topic, payment.getOrderId().toString(), createPaymentEvent);
    }

}
