package com.ecommerce.inventoryservice.dto;

public class OrderItemEvent {
    private Long productId;
    private Integer quantity;

    public OrderItemEvent() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}