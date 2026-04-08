package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handlePaymentCompleted_success_savesNotification() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_SUCCESS");
        event.setTransactionId("txn-xyz");

        notificationService.handlePaymentCompleted(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void handlePaymentCompleted_failed_savesFailureNotification() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(2L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_FAILED");
        event.setTransactionId(null);

        notificationService.handlePaymentCompleted(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void handleShipmentScheduled_savesNotification() {
        ShipmentScheduledEvent event = new ShipmentScheduledEvent();
        event.setOrderId(3L);
        event.setCustomerId(100L);
        event.setTrackingNumber("TRK-ABC123");
        event.setStatus("SHIPMENT_SCHEDULED");

        notificationService.handleShipmentScheduled(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
