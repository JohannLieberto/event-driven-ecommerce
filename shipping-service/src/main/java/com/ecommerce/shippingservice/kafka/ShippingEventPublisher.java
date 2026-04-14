package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.event.ShipmentScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishShipmentScheduled(Shipment shipment) {
        ShipmentScheduledEvent event = new ShipmentScheduledEvent(
            shipment.getOrderId(),
            shipment.getCustomerId(),
            shipment.getTrackingNumber(),
            shipment.getStatus()
        );
        kafkaTemplate.send("shipment.scheduled", String.valueOf(shipment.getOrderId()), event);
        log.info("Published shipment.scheduled for orderId={}", shipment.getOrderId());
    }
}
