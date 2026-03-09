package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // CREATE PRODUCT
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ALL PRODUCTS
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {

        Page<ProductResponse> products =
                inventoryService.getAllProducts(pageable);

        return ResponseEntity.ok(products);
    }

    // GET SINGLE PRODUCT
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        ProductResponse response = inventoryService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    // UPDATE PRODUCT
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response =
                inventoryService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    // DELETE PRODUCT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {

        inventoryService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ========== NEW ENDPOINTS FOR ORDER SERVICE INTEGRATION ==========

    /**
     * Check if sufficient stock is available
     * GET /api/inventory/{productId}/check?quantity=5
     */
    @GetMapping("/{productId}/check")
    public ResponseEntity<StockCheckResponse> checkStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        
        StockCheckResponse response = inventoryService.checkStock(productId, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Reserve stock for an order
     * PUT /api/inventory/{productId}/reserve
     * Body: {"quantity": 5, "orderId": 123}
     */
    @PutMapping("/{productId}/reserve")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        
        ProductResponse response = inventoryService.reserveStock(productId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Release reserved stock (e.g., if order is cancelled)
     * PUT /api/inventory/{productId}/release
     * Body: {"quantity": 5, "orderId": 123}
     */
    @PutMapping("/{productId}/release")
    public ResponseEntity<ProductResponse> releaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        
        ProductResponse response = inventoryService.releaseStock(productId, request);
        return ResponseEntity.ok(response);
    }

    // ========== BULK UPDATE ENDPOINT ==========

    @PostMapping("/bulk")
    public ResponseEntity<BulkUpdateResponse> bulkUpdateStock(
            @Valid @RequestBody BulkUpdateRequest request) {

        BulkUpdateResponse response = inventoryService.bulkUpdateStock(request);

        if (response.getFailureCount() > 0) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
