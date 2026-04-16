package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.dto.StockReservationRequest;
import com.ecommerce.inventoryservice.event.InventoryReservedEvent;
import com.ecommerce.inventoryservice.event.PaymentCompletedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryEventPublisher inventoryEventPublisher;

    @KafkaListener(
        topics = "payment-completed",
        groupId = "inventory-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[INVENTORY-SERVICE] Received payment.completed topic={} partition={} offset={} orderId={}",
            topic, partition, offset, event.getOrderId());

        if (!"SUCCESS".equals(event.getStatus())) {
            log.warn("[INVENTORY-SERVICE] Skipping inventory reservation for orderId={} — payment status={}",
                event.getOrderId(), event.getStatus());
            return;
        }

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[INVENTORY-SERVICE] No items in payment.completed event for orderId={}, skipping.",
                event.getOrderId());
            return;
        }

        try {
            for (PaymentCompletedEvent.OrderItemEvent item : event.getItems()) {
                StockReservationRequest reservationRequest = new StockReservationRequest();
                reservationRequest.setQuantity(item.getQuantity());
                reservationRequest.setOrderId(event.getOrderId());
                inventoryService.reserveStock(item.getProductId(), reservationRequest);
            }

            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                event.getOrderId(),
                event.getCustomerId(),
                "INVENTORY_RESERVED",
                "All items reserved successfully",
                LocalDateTime.now()
            );
            inventoryEventPublisher.publishInventoryReserved(reservedEvent);

        } catch (InsufficientStockException ex) {
            log.error("[INVENTORY-SERVICE] Insufficient stock for orderId={}: {}",
                event.getOrderId(), ex.getMessage());

            InventoryReservedEvent failedEvent = new InventoryReservedEvent(
                event.getOrderId(),
                event.getCustomerId(),
                "INVENTORY_RESERVATION_FAILED",
                ex.getMessage(),
                LocalDateTime.now()
            );
            inventoryEventPublisher.publishInventoryReserved(failedEvent);
        }
    }
}
