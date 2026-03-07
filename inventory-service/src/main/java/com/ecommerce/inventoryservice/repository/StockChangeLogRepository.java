package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.StockChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockChangeLogRepository extends JpaRepository<StockChangeLog, Long> {

    List<StockChangeLog> findByProductIdOrderByTimestampDesc(Long productId);

}
