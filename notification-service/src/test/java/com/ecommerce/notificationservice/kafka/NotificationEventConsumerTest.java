package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.event.InventoryReservedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void handlePaymentCompleted_delegatesToNotificationService() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_SUCCESS");
        event.setTransactionId("txn-abc");

        consumer.handlePaymentCompleted(event, "payment-completed");

        verify(notificationService).handlePaymentCompleted(event);
    }

    @Test
    void handleShipmentScheduled_delegatesToNotificationService() {
        ShipmentScheduledEvent event = new ShipmentScheduledEvent();
        event.setOrderId(2L);
        event.setCustomerId(100L);
        event.setTrackingNumber("TRK-123");
        event.setStatus("SHIPMENT_SCHEDULED");

        consumer.handleShipmentScheduled(event, "shipment.scheduled");

        verify(notificationService).handleShipmentScheduled(event);
    }

    @Test
    void handleInventoryReserved_delegatesToNotificationService() {
        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setOrderId(3L);
        event.setCustomerId(100L);
        event.setStatus("INVENTORY_RESERVED");
        event.setMessage("All items reserved");

        consumer.handleInventoryReserved(event, "inventory.reserved");

        verify(notificationService).handleInventoryReserved(event);
    }
}
