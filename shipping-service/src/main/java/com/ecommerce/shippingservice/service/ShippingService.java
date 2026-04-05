package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.event.ShipmentScheduledEvent;
import com.ecommerce.shippingservice.kafka.ShippingEventPublisher;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShippingEventPublisher shippingEventPublisher;

    public void scheduleShipment(PaymentCompletedEvent event) {
        log.info("[SHIPPING-SERVICE] Scheduling shipment for orderId={} customerId={}",
            event.getOrderId(), event.getCustomerId());

        // Idempotency check
        if (shipmentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("[SHIPPING-SERVICE] Shipment already scheduled for orderId={}, skipping.",
                event.getOrderId());
            return;
        }

        // Only proceed if payment was successful
        if (!"PAYMENT_SUCCESS".equals(event.getStatus())) {
            log.warn("[SHIPPING-SERVICE] Payment not successful for orderId={}, status={}. Skipping shipment.",
                event.getOrderId(), event.getStatus());
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(event.getOrderId());
        shipment.setCustomerId(event.getCustomerId());
        shipment.setStatus("PENDING");
        shipment = shipmentRepository.save(shipment);

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setStatus("SHIPMENT_SCHEDULED");
        Shipment savedShipment = shipmentRepository.save(shipment);

        log.info("[SHIPPING-SERVICE] Shipment SCHEDULED for orderId={} trackingNumber={}",
            savedShipment.getOrderId(), savedShipment.getTrackingNumber());

        ShipmentScheduledEvent scheduledEvent = new ShipmentScheduledEvent(
            savedShipment.getOrderId(),
            savedShipment.getCustomerId(),
            savedShipment.getTrackingNumber(),
            savedShipment.getStatus(),
            LocalDateTime.now()
        );

        shippingEventPublisher.publishShipmentScheduled(scheduledEvent);
    }
}
