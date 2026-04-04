package com.ecommerce.paymentservice.listener;

import com.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedListener {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "order-placed",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for orderId={}", event.getOrderId());
        paymentService.processPayment(event);
    }
}
