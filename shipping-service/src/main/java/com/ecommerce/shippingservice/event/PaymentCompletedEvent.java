package com.ecommerce.shippingservice.event;

import lombok.Data;

@Data
public class PaymentCompletedEvent {
    private Long orderId;
    private Long customerId;
    private String status;
    private String transactionId;
}
