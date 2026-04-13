package com.ecommerce.paymentservice.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;
}
