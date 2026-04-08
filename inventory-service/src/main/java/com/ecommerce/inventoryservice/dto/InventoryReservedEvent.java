package com.ecommerce.inventoryservice.dto;

import java.util.List;

public class InventoryReservedEvent {
    private Long orderId;
    private List<OrderItemEvent> items;

    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(Long orderId, List<OrderItemEvent> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public Long getOrderId() {
        return orderId;
    }

    public List<OrderItemEvent> getItems() {
        return items;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setItems(List<OrderItemEvent> items) {
        this.items = items;
    }
}