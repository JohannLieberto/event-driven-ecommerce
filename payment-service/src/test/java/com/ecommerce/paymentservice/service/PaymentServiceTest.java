package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_success_publishesCompletedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PENDING");

        when(paymentRepository.findFirstByOrderId(1L)).thenReturn(Optional.empty());

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setOrderId(1L);
        savedPayment.setCustomerId(100L);
        savedPayment.setStatus("PAYMENT_SUCCESS");

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.processPayment(event);

        verify(paymentRepository).findFirstByOrderId(1L);
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventPublisher).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_idempotent_skipsIfAlreadyProcessed() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PENDING");

        Payment existing = new Payment();
        existing.setOrderId(1L);
        existing.setStatus("PAYMENT_SUCCESS");

        when(paymentRepository.findFirstByOrderId(1L)).thenReturn(Optional.of(existing));

        paymentService.processPayment(event);

        verify(paymentRepository).findFirstByOrderId(1L);
        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }
}