package com.ecommerce.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long customerId;   // ✅ REQUIRED

    private Double amount;

    private String status;

    private String transactionId; // ✅ REQUIRED
}