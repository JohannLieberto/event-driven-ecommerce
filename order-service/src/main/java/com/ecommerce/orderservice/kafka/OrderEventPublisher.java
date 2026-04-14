package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String TOPIC = "orders.order-created";

    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[ORDER-SERVICE] Failed to publish order.created for orderId={}: {}",
                        event.getOrderId(), ex.getMessage());
            } else {
                log.info("[ORDER-SERVICE] Published order.created for orderId={} to topic={} partition={} offset={}",
                        event.getOrderId(),
                        TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
