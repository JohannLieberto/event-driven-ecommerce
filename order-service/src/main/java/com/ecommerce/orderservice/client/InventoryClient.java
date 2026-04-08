package com.ecommerce.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.InventoryServiceException;

import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Component
public class InventoryClient {

    @Autowired
    private RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service/api/inventory";

    /**
     * Check if product has sufficient stock.
     * Protected by inventoryCheckCircuitBreaker — falls back gracefully if inventory-service is down.
     */
    @CircuitBreaker(name = "inventoryCheckCircuitBreaker", fallbackMethod = "checkStockFallback")
    public boolean checkStock(Long productId, int quantity) {
        String url = INVENTORY_SERVICE_URL + "/" + productId + "/check?quantity=" + quantity;
        try {
            StockCheckResponse response = restTemplate.getForObject(url, StockCheckResponse.class);
            return response != null && response.isSufficient();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            throw new InventoryServiceException("Failed to check stock for product " + productId, e);
        }
    }

    /**
     * Fallback when inventoryCheckCircuitBreaker is open.
     * Returns false so order placement is rejected safely rather than overselling.
     */
    public boolean checkStockFallback(Long productId, int quantity, Throwable ex) {
        return false;
    }

    /**
     * Reserve stock for an order.
     * Protected by inventoryReserveCircuitBreaker — falls back by throwing a meaningful error.
     */
    @CircuitBreaker(name = "inventoryReserveCircuitBreaker", fallbackMethod = "reserveStockFallback")
    public void reserveStock(Long productId, int quantity, Long orderId) {
        String url = INVENTORY_SERVICE_URL + "/" + productId + "/reserve";
        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(quantity);
        request.setOrderId(orderId);
        try {
            restTemplate.put(url, request);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException("Insufficient stock for product " + productId);
        } catch (Exception e) {
            throw new InventoryServiceException("Failed to reserve stock for product " + productId, e);
        }
    }

    /**
     * Fallback when inventoryReserveCircuitBreaker is open.
     */
    public void reserveStockFallback(Long productId, int quantity, Long orderId, Throwable ex) {
        throw new InventoryServiceException(
                "Inventory service is temporarily unavailable. Please try again later. Product: " + productId, ex);
    }

    // ---- Inner DTOs ----

    public static class StockCheckResponse {
        private Long productId;
        private Integer availableStock;
        private boolean sufficient;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getAvailableStock() { return availableStock; }
        public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
        public boolean isSufficient() { return sufficient; }
        public void setSufficient(boolean sufficient) { this.sufficient = sufficient; }
    }

    public static class StockReservationRequest {
        private Integer quantity;
        private Long orderId;

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}
