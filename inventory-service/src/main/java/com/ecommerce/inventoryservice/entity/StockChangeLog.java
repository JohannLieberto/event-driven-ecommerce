package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_change_log")
public class StockChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "change_type")
    private String changeType; // RESERVE, RELEASE, BULK_UPDATE

    @Column(name = "quantity_changed")
    private Integer quantityChanged;

    @Column(name = "stock_before")
    private Integer stockBefore;

    @Column(name = "stock_after")
    private Integer stockAfter;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    public Long getId() { return id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String type) { this.changeType = type; }

    public Integer getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(Integer qty) { this.quantityChanged = qty; }

    public Integer getStockBefore() { return stockBefore; }
    public void setStockBefore(Integer before) { this.stockBefore = before; }

    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer after) { this.stockAfter = after; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public LocalDateTime getTimestamp() { return timestamp; }
}
