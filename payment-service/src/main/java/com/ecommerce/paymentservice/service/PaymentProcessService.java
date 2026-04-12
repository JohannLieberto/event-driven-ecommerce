package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.event.OrderCreatedEvent;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProcessService {

    private final PaymentRepository paymentRepository;

    public void processPayment(OrderCreatedEvent event) {

        Payment payment = new Payment();

        payment.setOrderId(event.getOrderId());
        payment.setCustomerId(event.getCustomerId()); // must be Long
        payment.setAmount(event.getAmount());

        payment.setStatus("SUCCESS");

        payment.setTransactionId(UUID.randomUUID().toString());

        paymentRepository.save(payment);
    }
}