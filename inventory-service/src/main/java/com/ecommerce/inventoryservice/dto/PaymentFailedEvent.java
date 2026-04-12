package com.ecommerce.inventoryservice.dto;

import java.util.List;

public class PaymentFailedEvent {
    private Long orderId;
    private List<OrderItem> items;


    public PaymentFailedEvent() {
    }

    public static class OrderItem {
        private Long productId;
        private int quantity;

        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }

        public void setProductId(Long productId) { this.productId = productId; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    public Long getOrderId() {
        return orderId;
    }
    public List<OrderItem> getItems() { return items; }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public void setItems(List<OrderItem> items) { this.items = items; }

}