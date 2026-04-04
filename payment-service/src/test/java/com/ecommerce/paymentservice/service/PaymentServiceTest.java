package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processPayment_success_publishesCompletedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PENDING");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setOrderId(1L);
        savedPayment.setCustomerId(100L);
        savedPayment.setStatus("PAYMENT_SUCCESS");
        savedPayment.setTransactionId("txn-123");

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        doNothing().when(paymentEventPublisher).publishPaymentCompleted(any());

        paymentService.processPayment(event);

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_idempotent_skipsIfAlreadyProcessed() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);

        Payment existing = new Payment();
        existing.setOrderId(1L);
        existing.setStatus("PAYMENT_SUCCESS");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }
}
