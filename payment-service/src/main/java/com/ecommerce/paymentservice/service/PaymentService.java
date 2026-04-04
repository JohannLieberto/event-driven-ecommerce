package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.ecommerce.paymentservice.event.PaymentProcessedEvent;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String CIRCUIT_BREAKER_NAME = "paymentProcessor";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "processPaymentFallback")
    public Payment processPayment(OrderPlacedEvent event) {
        log.info("Processing payment for orderId={}", event.getOrderId());

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getTotalAmount())
                .paymentMethod(event.getPaymentMethod())
                .status(Payment.PaymentStatus.PROCESSING)
                .transactionReference(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        // Simulate payment gateway call (replace with real gateway in production)
        boolean paymentSuccess = simulatePaymentGateway(event.getTotalAmount());

        if (paymentSuccess) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }

        payment = paymentRepository.save(payment);

        PaymentProcessedEvent processedEvent = PaymentProcessedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionReference(payment.getTransactionReference())
                .processedAt(payment.getProcessedAt())
                .build();

        kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, String.valueOf(event.getOrderId()), processedEvent);
        log.info("Published PaymentProcessedEvent for orderId={} status={}", event.getOrderId(), payment.getStatus());

        return payment;
    }

    public Payment processPaymentFallback(OrderPlacedEvent event, Throwable t) {
        log.error("Circuit breaker triggered for payment processing, orderId={}: {}", event.getOrderId(), t.getMessage());
        Payment failed = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getTotalAmount())
                .paymentMethod(event.getPaymentMethod())
                .status(Payment.PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();
        return paymentRepository.save(failed);
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for orderId: " + orderId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    private boolean simulatePaymentGateway(java.math.BigDecimal amount) {
        // Always succeeds in dev; swap for real gateway integration
        return true;
    }
}
