package com.ecommerce.orderservice.client;

/**
 * Port interface for inventory operations.
 * Using an interface allows easy mocking in unit/integration tests
 * without byte-buddy agent requirements on Java 21+.
 */
public interface InventoryClientPort {

    /**
     * Check if sufficient stock is available for a product.
     *
     * @param productId product to check
     * @param quantity  required quantity
     * @return true if stock is sufficient, false otherwise
     */
    boolean checkStock(Long productId, int quantity);

    /**
     * Reserve stock for a confirmed order.
     *
     * @param productId product to reserve
     * @param quantity  quantity to reserve
     * @param orderId   order placing the reservation
     */
    void reserveStock(Long productId, int quantity, Long orderId);
}
