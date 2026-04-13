package com.ecommerce.paymentservice.event;

import lombok.Data;

@Data
public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;
    private String status;
}
