package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.kafka.ShippingEventPublisher;
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
    private final ShippingEventPublisher shippingEventPublisher;

    public void scheduleShipment(PaymentCompletedEvent event) {
        // Only process successful payments
        if (!"PAYMENT_SUCCESS".equals(event.getStatus())) {
            log.info("Skipping shipment for orderId={} due to status={}", event.getOrderId(), event.getStatus());
            return;
        }

        // Idempotency check
        shipmentRepository.findByOrderId(event.getOrderId()).ifPresentOrElse(
            existing -> log.info("Shipment already exists for orderId={}, skipping", event.getOrderId()),
            () -> {
                // First save: persist with SHIPMENT_SCHEDULED
                Shipment shipment = new Shipment();
                shipment.setOrderId(event.getOrderId());
                shipment.setCustomerId(event.getCustomerId());
                shipment.setStatus("SHIPMENT_SCHEDULED");
                shipment.setTrackingNumber(UUID.randomUUID().toString());
                Shipment saved = shipmentRepository.save(shipment);

                // Publish event
                shippingEventPublisher.publishShipmentScheduled(saved);

                // Second save: mark as SHIPPED
                saved.setStatus("SHIPPED");
                shipmentRepository.save(saved);

                log.info("Shipment scheduled for orderId={}", event.getOrderId());
            }
        );
    }
}
