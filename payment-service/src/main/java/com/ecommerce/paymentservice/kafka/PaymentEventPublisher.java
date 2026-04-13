package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-completed:payment-completed}")
    private String paymentCompletedTopic;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent for orderId={}", event.getOrderId());
        kafkaTemplate.send(paymentCompletedTopic, String.valueOf(event.getOrderId()), event);
    }
}
