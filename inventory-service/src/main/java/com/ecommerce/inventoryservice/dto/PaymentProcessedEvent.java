package com.ecommerce.inventoryservice.dto;

public class PaymentProcessedEvent {
    private Long orderId;

    public PaymentProcessedEvent() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}