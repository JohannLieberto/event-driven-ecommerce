package com.example.shipping_service.listener;

import com.example.shipping_service.event.PaymentProcessedEvent;
import com.example.shipping_service.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProcessedListener {

    private final ShippingService shippingService;

    @KafkaListener(topics = "payments.payment-processed")
    public void listen(PaymentProcessedEvent event) {
        shippingService.scheduleShipment(event);
    }
}