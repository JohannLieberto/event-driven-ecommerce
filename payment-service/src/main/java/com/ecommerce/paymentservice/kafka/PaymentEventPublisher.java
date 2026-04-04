package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String TOPIC = "payment.completed";

    @Autowired
    private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        CompletableFuture<SendResult<String, PaymentCompletedEvent>> future =
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[PAYMENT-SERVICE] Failed to publish payment.completed for orderId={}: {}",
                    event.getOrderId(), ex.getMessage());
            } else {
                log.info("[PAYMENT-SERVICE] Published payment.completed for orderId={} partition={} offset={}",
                    event.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
