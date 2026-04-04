package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Payment.PaymentStatus;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_TOPIC = "payment-completed";

    @Transactional
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(PaymentStatus.PROCESSING)
                .transactionRef(UUID.randomUUID().toString())
                .build();

        payment = paymentRepository.save(payment);

        // Simulate payment gateway call - always succeeds in demo
        payment.setStatus(PaymentStatus.COMPLETED);
        payment = paymentRepository.save(payment);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .status("COMPLETED")
                .processedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(PAYMENT_TOPIC, String.valueOf(payment.getOrderId()), event);
        log.info("Payment completed and event published for order: {}", payment.getOrderId());

        return toResponse(payment);
    }

    public PaymentResponse paymentFallback(PaymentRequest request, Throwable t) {
        log.error("Payment circuit breaker triggered for order: {}. Error: {}", request.getOrderId(), t.getMessage());
        return PaymentResponse.builder()
                .orderId(request.getOrderId())
                .status(PaymentStatus.FAILED)
                .build();
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found for orderId: " + orderId));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .transactionRef(p.getTransactionRef())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
