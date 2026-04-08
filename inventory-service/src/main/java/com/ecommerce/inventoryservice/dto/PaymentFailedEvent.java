package com.ecommerce.inventoryservice.dto;

public class PaymentFailedEvent {
    private Long orderId;

    public PaymentFailedEvent() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}