package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;

import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.entity.StockChangeLog;

import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.exception.StockConcurrencyException;

import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.repository.StockChangeLogRepository;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockChangeLogRepository stockChangeLogRepository;

    // CREATE PRODUCT
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        Product saved = productRepository.save(product);

        return mapToResponse(saved);
    }

    // GET PRODUCT
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        return mapToResponse(product);
    }

    // UPDATE PRODUCT
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(product);

        return mapToResponse(updated);
    }

    // DELETE PRODUCT
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        productRepository.delete(product);
    }

    // CHECK STOCK
    public StockCheckResponse checkStock(Long productId, Integer requestedQuantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        StockCheckResponse response = new StockCheckResponse();
        response.setProductId(productId);
        response.setAvailableStock(product.getStockQuantity());
        response.setSufficient(product.getStockQuantity() >= requestedQuantity);

        return response;
    }

    // RESERVE STOCK
    // RESERVE STOCK
    public ProductResponse reserveStock(Long productId, StockReservationRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock");
        }

        int before = product.getStockQuantity();

        product.setStockQuantity(before - request.getQuantity());

        try {

            Product updated = productRepository.save(product);

            logStockChange(productId, "RESERVE",
                    request.getQuantity(),
                    before,
                    updated.getStockQuantity(),
                    request.getOrderId());

            return mapToResponse(updated);

        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    // RELEASE STOCK
    public ProductResponse releaseStock(Long productId, StockReservationRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        int before = product.getStockQuantity();

        product.setStockQuantity(before + request.getQuantity());

        try {

            Product updated = productRepository.save(product);

            logStockChange(productId, "RELEASE",
                    request.getQuantity(),
                    before,
                    updated.getStockQuantity(),
                    request.getOrderId());

            return mapToResponse(updated);

        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    // LOG STOCK CHANGE
    private void logStockChange(Long productId, String type,
                                Integer qty, Integer before,
                                Integer after, Long orderId) {

        StockChangeLog log = new StockChangeLog();

        log.setProductId(productId);
        log.setChangeType(type);
        log.setQuantityChanged(qty);
        log.setStockBefore(before);
        log.setStockAfter(after);
        log.setOrderId(orderId);

        stockChangeLogRepository.save(log);
    }

    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());

        return response;
    }



    public Page<ProductResponse> getAllProducts(Pageable pageable) {

        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public BulkUpdateResponse bulkUpdateStock(BulkUpdateRequest request) {

        List<UpdateResult> results = new ArrayList<>();

        int successCount = 0;
        int failureCount = 0;

        for (ProductStockUpdate update : request.getUpdates()) {

            try {

                Product product = productRepository.findById(update.getProductId())
                        .orElseThrow(() ->
                                new ProductNotFoundException("Product not found: " + update.getProductId()));

                Integer stockBefore = product.getStockQuantity();

                product.setStockQuantity(update.getNewQuantity());

                productRepository.save(product);

                int diff = update.getNewQuantity() - stockBefore;

                logStockChange(product.getId(),
                        "BULK_UPDATE",
                        Math.abs(diff),
                        stockBefore,
                        update.getNewQuantity(),
                        null);

                UpdateResult result = new UpdateResult();

                result.setProductId(update.getProductId());
                result.setSuccess(true);
                result.setMessage("Updated successfully");
                result.setNewQuantity(update.getNewQuantity());

                results.add(result);

                successCount++;

            } catch (ProductNotFoundException e) {

                UpdateResult result = new UpdateResult();

                result.setProductId(update.getProductId());
                result.setSuccess(false);
                result.setMessage(e.getMessage());

                results.add(result);

                failureCount++;
            }
        }

        BulkUpdateResponse response = new BulkUpdateResponse();

        response.setTotalRequested(request.getUpdates().size());
        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setResults(results);

        return response;
    }

}