package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderId(Long orderId);

    List<InventoryReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, ReservationStatus status);

    Optional<InventoryReservation> findByOrderIdAndProductId(Long orderId, Long productId);
}