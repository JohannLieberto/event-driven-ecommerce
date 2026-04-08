package com.ecommerce.inventoryservice.dto;

public class InventoryFailedEvent {
    private Long orderId;
    private String reason;

    public InventoryFailedEvent() {
    }

    public InventoryFailedEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}