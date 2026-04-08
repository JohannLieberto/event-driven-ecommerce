package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentEventPublisher paymentEventPublisher;

    public Payment createPayment(Payment payment) {
        payment.setStatus("SUCCESS");
        return repository.save(payment);
    }

    public void processPayment(OrderCreatedEvent event) {

        // ✅ Idempotency check
        if (repository.findByOrderId(event.getOrderId()).isPresent()) {
            return;
        }

        // ✅ Create payment
        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setCustomerId(event.getCustomerId());
        payment.setAmount(event.getAmount());
        payment.setStatus("SUCCESS");

        // ✅ Save to DB
        repository.save(payment);

        // ✅ Publish event (THIS FIXES YOUR TEST)
        // Create event
        com.ecommerce.paymentservice.event.PaymentCompletedEvent completedEvent =
                new com.ecommerce.paymentservice.event.PaymentCompletedEvent();
        completedEvent.setOrderId(payment.getOrderId());
        completedEvent.setCustomerId(payment.getCustomerId());
        completedEvent.setStatus(payment.getStatus());

        // Publish event
        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }
}