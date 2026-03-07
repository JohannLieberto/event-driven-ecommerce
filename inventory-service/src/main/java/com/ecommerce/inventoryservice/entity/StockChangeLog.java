package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_change_log")
public class StockChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private String changeType; // RESERVE, RELEASE

    private Integer quantityChanged;

    private Integer stockBefore;

    private Integer stockAfter;

    private Long orderId;

    private LocalDateTime timestamp = LocalDateTime.now();

    public void setProductId(Long productId) {
    }

    public void setChangeType(String type) {
    }

    public void setQuantityChanged(Integer qty) {
    }

    public void setStockBefore(Integer before) {
    }

    public void setStockAfter(Integer after) {
    }

    public void setOrderId(Long orderId) {
    }

    // getters and setters
}
