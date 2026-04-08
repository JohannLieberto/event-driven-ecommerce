package com.ecommerce.shippingservice.event;

import java.time.LocalDateTime;

public class ShipmentScheduledEvent {

    private Long orderId;
    private Long customerId;
    private String trackingNumber;
    private String status; // SHIPMENT_SCHEDULED
    private LocalDateTime scheduledAt;

    public ShipmentScheduledEvent() {}

    public ShipmentScheduledEvent(Long orderId, Long customerId, String trackingNumber,
                                   String status, LocalDateTime scheduledAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
