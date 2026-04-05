package com.ecommerce.paymentservice.event;

import java.time.LocalDateTime;

public class PaymentCompletedEvent {

    private Long orderId;
    private Long customerId;
    private String status; // PAYMENT_SUCCESS or PAYMENT_FAILED
    private String transactionId;
    private LocalDateTime processedAt;

    public PaymentCompletedEvent() {}

    public PaymentCompletedEvent(Long orderId, Long customerId, String status,
                                  String transactionId, LocalDateTime processedAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.transactionId = transactionId;
        this.processedAt = processedAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
