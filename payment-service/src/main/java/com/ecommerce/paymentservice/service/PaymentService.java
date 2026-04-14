package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * Called by PaymentController via REST for direct payment creation.
     */
    public Payment createPayment(Payment payment) {
        payment.setStatus("PAYMENT_SUCCESS");
        return paymentRepository.save(payment);
    }

    /**
     * Called by Kafka listener for event-driven payment processing.
     * Idempotent: skips if already processed.
     */
    public void processPayment(OrderCreatedEvent event) {
        if (!"PENDING".equals(event.getStatus())) {
            log.info("Skipping non-PENDING order {}", event.getOrderId());
            return;
        }

        paymentRepository.findByOrderId(event.getOrderId()).ifPresentOrElse(
            existing -> log.info("Payment already processed for orderId={}, skipping", event.getOrderId()),
            () -> {
                Payment payment = new Payment();
                payment.setOrderId(event.getOrderId());
                payment.setCustomerId(event.getCustomerId());
                payment.setAmount(event.getAmount());
                payment.setStatus("PAYMENT_SUCCESS");

                paymentRepository.save(payment);

                paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(event.getOrderId(), event.getCustomerId(), "PAYMENT_SUCCESS")
                );
            }
        );
    }
}
