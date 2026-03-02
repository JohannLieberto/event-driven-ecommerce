package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<product, Long> {
}