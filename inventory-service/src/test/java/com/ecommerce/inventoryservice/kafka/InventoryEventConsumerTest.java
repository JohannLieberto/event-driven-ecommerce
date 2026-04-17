package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.dto.StockReservationRequest;
import com.ecommerce.inventoryservice.event.PaymentCompletedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    @InjectMocks
    private InventoryEventConsumer consumer;

    @Test
    void handlePaymentCompleted_success_reservesStockAndPublishesReservedEvent() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", List.of(item(1L, 2), item(2L, 1)));

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verify(inventoryService).reserveStock(eq(1L), any(StockReservationRequest.class));
        verify(inventoryService).reserveStock(eq(2L), any(StockReservationRequest.class));
        verify(inventoryEventPublisher).publishInventoryReserved(
                argThat(e -> "INVENTORY_RESERVED".equals(e.getStatus())));
    }

    @Test
    void handlePaymentCompleted_paymentNotSuccess_skipsReservation() {
        PaymentCompletedEvent event = buildEvent("PAYMENT_FAILED", List.of(item(1L, 2)));

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(inventoryEventPublisher);
    }

    @Test
    void handlePaymentCompleted_nullItems_skipsReservation() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", null);

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(inventoryEventPublisher);
    }

    @Test
    void handlePaymentCompleted_emptyItems_skipsReservation() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", Collections.emptyList());

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(inventoryEventPublisher);
    }

    @Test
    void handlePaymentCompleted_insufficientStock_publishesFailureEvent() {
        PaymentCompletedEvent event = buildEvent("SUCCESS", List.of(item(1L, 9999)));

        doThrow(new InsufficientStockException("Insufficient stock"))
                .when(inventoryService).reserveStock(eq(1L), any(StockReservationRequest.class));

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verify(inventoryEventPublisher).publishInventoryReserved(
                argThat(e -> "INVENTORY_RESERVATION_FAILED".equals(e.getStatus())));
    }

    private PaymentCompletedEvent buildEvent(String status, List<PaymentCompletedEvent.OrderItemEvent> items) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus(status);
        event.setItems(items);
        return event;
    }

    private PaymentCompletedEvent.OrderItemEvent item(Long productId, int qty) {
        PaymentCompletedEvent.OrderItemEvent item = new PaymentCompletedEvent.OrderItemEvent();
        item.setProductId(productId);
        item.setQuantity(qty);
        return item;
    }
}
