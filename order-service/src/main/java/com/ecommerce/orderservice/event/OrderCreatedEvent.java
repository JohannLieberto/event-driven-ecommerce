package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.dto.OrderItemEvent;

import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;
    private String status;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId,
                             Long customerId,
                             String status,
                             List<OrderItemEvent> items,
                             LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItemEvent> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEvent> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}