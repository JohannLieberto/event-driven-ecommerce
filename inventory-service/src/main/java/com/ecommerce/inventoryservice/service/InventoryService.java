package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.entity.StockChangeLog;
import com.ecommerce.inventoryservice.event.InventoryReservedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.exception.StockConcurrencyException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.repository.StockChangeLogRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String PRODUCT_NOT_FOUND = "Product not found";

    private final ProductRepository productRepository;
    private final StockChangeLogRepository stockChangeLogRepository;

    public InventoryService(ProductRepository productRepository,
                            StockChangeLogRepository stockChangeLogRepository) {
        this.productRepository = productRepository;
        this.stockChangeLogRepository = stockChangeLogRepository;
    }

    // ===================== PRODUCT CRUD =====================

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        return mapToResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        return mapToResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        productRepository.delete(product);
    }

    // ===================== STOCK =====================

    public StockCheckResponse checkStock(Long productId, Integer requestedQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        StockCheckResponse response = new StockCheckResponse();
        response.setProductId(productId);
        response.setAvailableStock(product.getStockQuantity());
        response.setSufficient(product.getStockQuantity() >= requestedQuantity);
        return response;
    }

    public ProductResponse reserveStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock");
        }

        int before = product.getStockQuantity();

        try {
            product.setStockQuantity(before - request.getQuantity());

            Product updated = productRepository.save(product);

            logStockChange(productId, "RESERVE",
                    request.getQuantity(), before,
                    updated.getStockQuantity(), request.getOrderId());

            log.info("✅ Stock reserved for orderId={}", request.getOrderId());

            return mapToResponse(updated);

        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    public ProductResponse releaseStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        int before = product.getStockQuantity();

        product.setStockQuantity(before + request.getQuantity());

        Product updated = productRepository.save(product);

        logStockChange(productId, "RELEASE",
                request.getQuantity(), before,
                updated.getStockQuantity(), request.getOrderId());

        return mapToResponse(updated);
    }

    public ProductResponse addStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        int before = product.getStockQuantity();
        product.setStockQuantity(before + request.getQuantity());

        Product updated = productRepository.save(product);

        logStockChange(productId, "ADD",
                request.getQuantity(), before,
                updated.getStockQuantity(), request.getOrderId());

        return mapToResponse(updated);
    }

    // ===================== EVENT HANDLERS =====================

    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[INVENTORY-SERVICE] Handling order.created for orderId={}", event.getOrderId());
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[INVENTORY-SERVICE] No items in order.created event for orderId={}", event.getOrderId());
            return;
        }
        for (OrderItemEvent item : event.getItems()) {
            StockReservationRequest req = new StockReservationRequest();
            req.setQuantity(item.getQuantity());
            req.setOrderId(event.getOrderId());

            reserveStock(item.getProductId(), req);
        }
    }

    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("💰 Payment processed for orderId={}", event.getOrderId());
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("❌ Payment failed, releasing stock for orderId={}", event.getOrderId());

        for (PaymentFailedEvent.OrderItem item : event.getItems()) {
            StockReservationRequest req = new StockReservationRequest();
            req.setQuantity(item.getQuantity());
            req.setOrderId(event.getOrderId());

            releaseStock(item.getProductId(), req);
        }
    }

    // ===================== BULK =====================

    public BulkUpdateResponse bulkUpdateStock(BulkUpdateRequest request) {

        List<UpdateResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (ProductStockUpdate update : request.getUpdates()) {
            try {
                Product product = productRepository.findById(update.getProductId())
                        .orElseThrow(() ->
                                new ProductNotFoundException(PRODUCT_NOT_FOUND + ": " + update.getProductId()));

                int before = product.getStockQuantity();
                product.setStockQuantity(update.getNewQuantity());

                productRepository.save(product);

                logStockChange(product.getId(), "BULK_UPDATE",
                        Math.abs(update.getNewQuantity() - before),
                        before,
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

    // ===================== LOGGING =====================

    private void logStockChange(Long productId, String type,
                                Integer qty, Integer before,
                                Integer after, Long orderId) {

        StockChangeLog logEntry = new StockChangeLog();
        logEntry.setProductId(productId);
        logEntry.setChangeType(type);
        logEntry.setQuantityChanged(qty);
        logEntry.setStockBefore(before);
        logEntry.setStockAfter(after);
        logEntry.setOrderId(orderId);

        stockChangeLogRepository.save(logEntry);
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
}
