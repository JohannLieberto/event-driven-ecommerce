package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shippingservice.event.ShipmentScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ShippingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShippingEventPublisher.class);
    private static final String TOPIC = "shipment.scheduled";

    @Autowired
    private KafkaTemplate<String, ShipmentScheduledEvent> kafkaTemplate;

    public void publishShipmentScheduled(ShipmentScheduledEvent event) {
        CompletableFuture<SendResult<String, ShipmentScheduledEvent>> future =
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[SHIPPING-SERVICE] Failed to publish shipment.scheduled for orderId={}: {}",
                    event.getOrderId(), ex.getMessage());
            } else {
                log.info("[SHIPPING-SERVICE] Published shipment.scheduled for orderId={} partition={} offset={}",
                    event.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
