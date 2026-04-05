package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.event.PaymentCompletedEvent;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    public void processPayment(OrderCreatedEvent event) {
        log.info("[PAYMENT-SERVICE] Processing payment for orderId={} customerId={}",
            event.getOrderId(), event.getCustomerId());

        // Check idempotency — do not process same order twice
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("[PAYMENT-SERVICE] Payment already processed for orderId={}, skipping.",
                event.getOrderId());
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setCustomerId(event.getCustomerId());
        payment.setStatus("PENDING");
        payment = paymentRepository.save(payment);

        // Simulate payment processing (always succeeds in demo)
        String transactionId = UUID.randomUUID().toString();
        payment.setTransactionId(transactionId);
        payment.setStatus("PAYMENT_SUCCESS");
        Payment savedPayment = paymentRepository.save(payment);

        log.info("[PAYMENT-SERVICE] Payment SUCCESS for orderId={} transactionId={}",
            savedPayment.getOrderId(), savedPayment.getTransactionId());

        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
            savedPayment.getOrderId(),
            savedPayment.getCustomerId(),
            savedPayment.getStatus(),
            savedPayment.getTransactionId(),
            LocalDateTime.now()
        );

        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }
}
