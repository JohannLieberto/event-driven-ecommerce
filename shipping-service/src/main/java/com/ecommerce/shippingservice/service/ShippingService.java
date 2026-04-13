package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    /**
     * Called by ShippingEventConsumer when a payment.completed Kafka event arrives.
     */
    public void scheduleShipment(PaymentCompletedEvent event) {
        processShipment(event);
    }

    /**
     * Idempotent shipment creation.
     */
    public void processShipment(PaymentCompletedEvent event) {
        shipmentRepository.findByOrderId(event.getOrderId()).ifPresentOrElse(
            existing -> log.info("Shipment already exists for orderId={}, skipping", event.getOrderId()),
            () -> {
                Shipment shipment = new Shipment();
                shipment.setOrderId(event.getOrderId());
                shipment.setCustomerId(event.getCustomerId());
                shipment.setStatus("SHIPPED");
                shipment.setTrackingNumber(UUID.randomUUID().toString());
                shipmentRepository.save(shipment);
                log.info("Shipment created for orderId={}", event.getOrderId());
            }
        );
    }
}
