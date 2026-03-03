package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;  // ← entity, NOT model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);  // ← Long, NOT String
    List<Order> findByStatus(String status);
}
