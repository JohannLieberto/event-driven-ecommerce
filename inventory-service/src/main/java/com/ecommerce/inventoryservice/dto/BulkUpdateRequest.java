package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BulkUpdateRequest {

    @NotEmpty(message = "Updates list cannot be empty")
    @Size(max = 100, message = "Bulk update limited to 100 products")
    private List<ProductStockUpdate> updates;

    public List<ProductStockUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<ProductStockUpdate> updates) {
        this.updates = updates;
    }
}