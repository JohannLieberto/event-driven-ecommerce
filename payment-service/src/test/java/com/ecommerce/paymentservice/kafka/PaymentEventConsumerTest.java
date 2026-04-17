package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    void handleOrderCreated_delegatesToPaymentService() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setAmount(BigDecimal.valueOf(99.99));
        event.setStatus("PENDING");

        consumer.handleOrderCreated(event, "orders.order-created", 0, 0L);

        verify(paymentService).processPayment(event);
    }
}
