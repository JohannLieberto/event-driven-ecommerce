package com.ecommerce.paymentservice.event;

import lombok.Data;

@Data
public class PaymentCompletedEvent {

    private Long orderId;
    private Long customerId;
    private String status;

    public PaymentCompletedEvent(Long orderId, Long customerId, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
    }
}
