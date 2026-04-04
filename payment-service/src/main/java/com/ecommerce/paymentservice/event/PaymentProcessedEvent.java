package com.ecommerce.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String status;       // COMPLETED or FAILED
    private String transactionReference;
    private LocalDateTime processedAt;
}
