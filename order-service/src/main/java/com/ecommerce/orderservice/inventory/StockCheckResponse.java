package com.ecommerce.orderservice.inventory;

public class StockCheckResponse {

    private Long productId;
    private Integer availableStock;
    private boolean sufficient;

    public StockCheckResponse() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public boolean isSufficient() {
        return sufficient;
    }

    public void setSufficient(boolean sufficient) {
        this.sufficient = sufficient;
    }
}
