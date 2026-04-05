package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class PaymentProcessService {

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        if (request.getOrderId() == null || request.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "orderId and customerId are required");
        }

        // Idempotency check
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            Payment existing = paymentRepository.findByOrderId(request.getOrderId()).get();
            return mapToResponse(existing);
        }

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setStatus("PENDING");
        payment = paymentRepository.save(payment);

        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setStatus("PAYMENT_SUCCESS");
        Payment saved = paymentRepository.save(payment);

        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(payment.getOrderId());
        response.setCustomerId(payment.getCustomerId());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        return response;
    }
}
