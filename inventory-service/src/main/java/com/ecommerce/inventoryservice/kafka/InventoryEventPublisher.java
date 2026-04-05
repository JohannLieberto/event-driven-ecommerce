package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.event.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);
    private static final String TOPIC_RESERVED = "inventory.reserved";
    private static final String TOPIC_FAILED = "inventory.reservation.failed";

    @Autowired
    private KafkaTemplate<String, InventoryReservedEvent> kafkaTemplate;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        String topic = "INVENTORY_RESERVED".equals(event.getStatus()) ? TOPIC_RESERVED : TOPIC_FAILED;
        CompletableFuture<SendResult<String, InventoryReservedEvent>> future =
            kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[INVENTORY-SERVICE] Failed to publish {} for orderId={}: {}",
                    topic, event.getOrderId(), ex.getMessage());
            } else {
                log.info("[INVENTORY-SERVICE] Published {} for orderId={} partition={} offset={}",
                    topic, event.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
