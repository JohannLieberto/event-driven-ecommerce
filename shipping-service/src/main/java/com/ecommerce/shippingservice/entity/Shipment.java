package com.ecommerce.shippingservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shipments")
@Data
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column
    private Long customerId;

    @Column(nullable = false)
    private String status;

    @Column
    private String trackingNumber;
}
