package com.ecommerce.payment;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    void processPayment_shouldSaveAndReturnCompleted() {
        PaymentRequest req = new PaymentRequest();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(99.99));

        Payment saved = Payment.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(99.99))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionRef("txn-test")
                .build();

        when(paymentRepository.save(any())).thenReturn(saved);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(null);

        PaymentResponse response = paymentService.processPayment(req);

        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(response.getOrderId()).isEqualTo(1L);
    }

    @Test
    void getPaymentByOrderId_shouldReturnPayment() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(42L)
                .amount(BigDecimal.valueOf(50.00))
                .status(Payment.PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByOrderId(42L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(42L);
        assertThat(response.getOrderId()).isEqualTo(42L);
    }
}
