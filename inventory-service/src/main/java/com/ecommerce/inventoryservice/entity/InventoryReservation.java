package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long productId;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createdAt;

    public InventoryReservation() {
    }

    public InventoryReservation(Long orderId, Long productId, Integer quantity, ReservationStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}