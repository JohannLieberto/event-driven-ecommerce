package com.ecommerce.inventoryservice.event;

import java.time.LocalDateTime;

public class InventoryReservedEvent {

    private Long orderId;
    private Long customerId;
    private String status; // INVENTORY_RESERVED or INVENTORY_RESERVATION_FAILED
    private String message;
    private LocalDateTime reservedAt;

    public InventoryReservedEvent() {}

    public InventoryReservedEvent(Long orderId, Long customerId, String status,
                                   String message, LocalDateTime reservedAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.message = message;
        this.reservedAt = reservedAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
}
