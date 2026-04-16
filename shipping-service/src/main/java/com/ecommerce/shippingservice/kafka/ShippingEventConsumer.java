package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ShippingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShippingEventConsumer.class);

    @Autowired
    private ShippingService shippingService;

    @KafkaListener(
        topics = "payment-completed",
        groupId = "shipping-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[SHIPPING-SERVICE] Received payment.completed from topic={} partition={} offset={} orderId={}",
            topic, partition, offset, event.getOrderId());

        shippingService.scheduleShipment(event);
    }
}
