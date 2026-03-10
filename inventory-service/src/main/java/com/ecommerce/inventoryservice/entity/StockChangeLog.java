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

    public void setProductId(Long productId) { this.productId = productId; }
    public void setChangeType(String type) { this.changeType = type; }
    public void setQuantityChanged(Integer qty) { this.quantityChanged = qty; }
    public void setStockBefore(Integer before) { this.stockBefore = before; }
    public void setStockAfter(Integer after) { this.stockAfter = after; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }


    // getters and setters
}
