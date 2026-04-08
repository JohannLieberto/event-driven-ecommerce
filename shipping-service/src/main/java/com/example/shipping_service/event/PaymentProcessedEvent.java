package com.example.shipping_service.event;

import lombok.Data;

@Data
public class PaymentProcessedEvent {

    private Long orderId;
    private Long paymentId;
    private Double amount;
    private String status;
    private String timestamp;
}