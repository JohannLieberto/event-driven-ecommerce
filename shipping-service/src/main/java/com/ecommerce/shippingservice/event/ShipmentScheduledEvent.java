package com.ecommerce.shippingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentScheduledEvent {
    private Long orderId;
    private Long customerId;
    private String trackingNumber;
    private String status;
}
