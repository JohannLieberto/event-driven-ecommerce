package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.service.ShippingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShippingEventConsumerTest {

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingEventConsumer consumer;

    @Test
    void handlePaymentCompleted_delegatesToShippingService() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_SUCCESS");
        event.setTransactionId("txn-abc");

        consumer.handlePaymentCompleted(event, "payment-completed", 0, 0L);

        verify(shippingService).scheduleShipment(event);
    }
}
