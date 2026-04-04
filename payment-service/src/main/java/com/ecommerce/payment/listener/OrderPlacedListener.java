package com.ecommerce.payment.listener;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.event.OrderPlacedEvent;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-placed", groupId = "payment-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order-placed event for orderId: {}", event.getOrderId());
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(event.getOrderId());
        request.setAmount(event.getTotalAmount());
        paymentService.processPayment(request);
    }
}
