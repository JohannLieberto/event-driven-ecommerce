package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.event.InventoryReservedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
        topics = "payment.completed",
        groupId = "notification-service-payment-group",
        containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[NOTIFICATION-SERVICE] Received payment.completed orderId={}", event.getOrderId());
        notificationService.handlePaymentCompleted(event);
    }

    @KafkaListener(
        topics = "shipment.scheduled",
        groupId = "notification-service-shipment-group",
        containerFactory = "shipmentKafkaListenerContainerFactory"
    )
    public void handleShipmentScheduled(
            @Payload ShipmentScheduledEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[NOTIFICATION-SERVICE] Received shipment.scheduled orderId={}", event.getOrderId());
        notificationService.handleShipmentScheduled(event);
    }

    @KafkaListener(
        topics = "inventory.reserved",
        groupId = "notification-service-inventory-group",
        containerFactory = "inventoryKafkaListenerContainerFactory"
    )
    public void handleInventoryReserved(
            @Payload InventoryReservedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[NOTIFICATION-SERVICE] Received inventory.reserved orderId={}", event.getOrderId());
        notificationService.handleInventoryReserved(event);
    }
}
