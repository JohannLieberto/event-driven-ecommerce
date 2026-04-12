package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
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
        payment.setStatus("PAYMENT_SUCCESS"); // ✅ standardized
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
        payment.setStatus("PAYMENT_SUCCESS");

        // ✅ Save to DB
        repository.save(payment);

        // ✅ Publish event
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent();
        completedEvent.setOrderId(payment.getOrderId());
        completedEvent.setCustomerId(payment.getCustomerId());
        completedEvent.setStatus(payment.getStatus());

        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }
}