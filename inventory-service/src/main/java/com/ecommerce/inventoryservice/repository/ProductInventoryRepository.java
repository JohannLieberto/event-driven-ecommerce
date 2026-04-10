package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
}