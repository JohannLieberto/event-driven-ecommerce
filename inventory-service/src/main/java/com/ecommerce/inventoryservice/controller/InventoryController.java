package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // HEALTH CHECK — must be declared before /{id} to avoid path collision
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("inventory-service is running");
    }

    // CREATE PRODUCT
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ALL PRODUCTS — returns plain List so Karate #array match works
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = inventoryService.getAllProducts();
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
        ProductResponse response = inventoryService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE PRODUCT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ADD STOCK — POST /api/inventory/{productId}/add
    @PostMapping("/{productId}/add")
    public ResponseEntity<ProductResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        ProductResponse response = inventoryService.addStock(productId, request);
        return ResponseEntity.ok(response);
    }

    // CHECK STOCK
    @GetMapping("/{productId}/check")
    public ResponseEntity<StockCheckResponse> checkStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        StockCheckResponse response = inventoryService.checkStock(productId, quantity);
        return ResponseEntity.ok(response);
    }

    // RESERVE STOCK — PUT /api/inventory/{productId}/reserve
    @PutMapping("/{productId}/reserve")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        ProductResponse response = inventoryService.reserveStock(productId, request);
        return ResponseEntity.ok(response);
    }

    // RELEASE STOCK — PUT /api/inventory/{productId}/release
    @PutMapping("/{productId}/release")
    public ResponseEntity<ProductResponse> releaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        ProductResponse response = inventoryService.releaseStock(productId, request);
        return ResponseEntity.ok(response);
    }

    // BULK UPDATE
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
