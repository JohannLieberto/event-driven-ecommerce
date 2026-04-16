package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        if (request.getOrderId() == null || request.getCustomerId() == null || request.getAmount() == null) {
            return ResponseEntity.badRequest().build();
        }

        return paymentRepository.findFirstByOrderId(request.getOrderId())
            .map(existing -> {
                // Payment already exists (e.g. from Kafka replay) — re-publish so downstream
                // services (shipping) are notified in case they missed the original event.
                // Shipping service has its own idempotency check so duplicate events are safe.
                paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(existing.getOrderId(), existing.getCustomerId(), "PAYMENT_SUCCESS")
                );
                return ResponseEntity.ok(toResponse(existing));
            })
            .orElseGet(() -> {
                Payment payment = new Payment();
                payment.setOrderId(request.getOrderId());
                payment.setCustomerId(request.getCustomerId());
                payment.setAmount(BigDecimal.valueOf(request.getAmount()));
                payment.setStatus("PAYMENT_SUCCESS");
                payment.setTransactionId(UUID.randomUUID().toString());
                paymentRepository.save(payment);

                paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(payment.getOrderId(), payment.getCustomerId(), "PAYMENT_SUCCESS")
                );

                return ResponseEntity.ok(toResponse(payment));
            });
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(payment.getOrderId());
        response.setCustomerId(payment.getCustomerId());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        return response;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentRepository.findFirstByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("payment-service is running");
    }
}
