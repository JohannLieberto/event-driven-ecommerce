package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.ProductRequest;
import com.ecommerce.inventoryservice.dto.ProductResponse;
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
}