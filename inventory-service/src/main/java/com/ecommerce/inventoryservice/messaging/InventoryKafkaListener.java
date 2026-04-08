package com.ecommerce.inventoryservice.messaging;

import com.ecommerce.inventoryservice.dto.OrderCreatedEvent;
import com.ecommerce.inventoryservice.dto.PaymentFailedEvent;
import com.ecommerce.inventoryservice.dto.PaymentProcessedEvent;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryKafkaListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public InventoryKafkaListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "orders.order-created", groupId = "inventory-service")
    public void onOrderCreated(String message) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
        inventoryService.handleOrderCreated(event);
    }

    @KafkaListener(topics = "payments.payment-processed", groupId = "inventory-service")
    public void onPaymentProcessed(String message) throws Exception {
        PaymentProcessedEvent event = objectMapper.readValue(message, PaymentProcessedEvent.class);
        inventoryService.handlePaymentProcessed(event);
    }

    @KafkaListener(topics = "payments.payment-failed", groupId = "inventory-service")
    public void onPaymentFailed(String message) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
        inventoryService.handlePaymentFailed(event);
    }
}