package com.ecommerce.inventoryservice.dto;

public class PaymentProcessedEvent {
    private Long orderId;
    private String status;


    public PaymentProcessedEvent() {
    }

    public Long getOrderId() {
        return orderId;
    }
    public String getStatus() { return status; }


    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public void setStatus(String status) { this.status = status; }

}