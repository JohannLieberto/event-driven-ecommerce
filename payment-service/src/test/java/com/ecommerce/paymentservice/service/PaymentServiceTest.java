package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_shouldSaveAndReturnCompletedPayment() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(1L)
                .customerId("cust-001")
                .totalAmount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment savedPayment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Payment result = paymentService.processPayment(event);

        assertNotNull(result);
        verify(paymentRepository, atLeast(1)).save(any(Payment.class));
    }
}
