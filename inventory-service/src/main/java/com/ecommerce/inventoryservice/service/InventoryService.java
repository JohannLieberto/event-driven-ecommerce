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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryService(ProductRepository productRepository,
                            StockChangeLogRepository stockChangeLogRepository,
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.productRepository = productRepository;
        this.stockChangeLogRepository = stockChangeLogRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

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

    // GET ALL PRODUCTS
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // GET PRODUCT
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    // UPDATE PRODUCT
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
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
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        productRepository.delete(product);
    }

    // ADD STOCK
    public ProductResponse addStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        int before = product.getStockQuantity();
        product.setStockQuantity(before + request.getQuantity());
        try {
            Product updated = productRepository.save(product);
            logStockChange(productId, "ADD", request.getQuantity(), before,
                    updated.getStockQuantity(), request.getOrderId());
            return mapToResponse(updated);
        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    // CHECK STOCK
    public StockCheckResponse checkStock(Long productId, Integer requestedQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        StockCheckResponse response = new StockCheckResponse();
        response.setProductId(productId);
        response.setAvailableStock(product.getStockQuantity());
        response.setSufficient(product.getStockQuantity() >= requestedQuantity);
        return response;
    }

    // RESERVE STOCK
    public ProductResponse reserveStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock");
        }
        int before = product.getStockQuantity();
        product.setStockQuantity(before - request.getQuantity());
        try {
            Product updated = productRepository.save(product);
            logStockChange(productId, "RESERVE", request.getQuantity(), before,
                    updated.getStockQuantity(), request.getOrderId());
            return mapToResponse(updated);
        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    // RELEASE STOCK
    public ProductResponse releaseStock(Long productId, StockReservationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        int before = product.getStockQuantity();
        product.setStockQuantity(before + request.getQuantity());
        try {
            Product updated = productRepository.save(product);
            logStockChange(productId, "RELEASE", request.getQuantity(), before,
                    updated.getStockQuantity(), request.getOrderId());
            return mapToResponse(updated);
        } catch (OptimisticLockException e) {
            throw new StockConcurrencyException("Stock modified by another request");
        }
    }

    // HANDLE ORDER CREATED (consumed by InventoryKafkaListener via orders.order-created)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[INVENTORY-SERVICE] Handling order.created for orderId={}", event.getOrderId());
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[INVENTORY-SERVICE] No items in order.created event for orderId={}", event.getOrderId());
            return;
        }
        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            StockReservationRequest req = new StockReservationRequest();
            req.setQuantity(item.getQuantity());
            req.setOrderId(event.getOrderId());
            reserveStock(item.getProductId(), req);
        }
    }

    // HANDLE PAYMENT PROCESSED (consumed by InventoryKafkaListener via payments.payment-processed)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("[INVENTORY-SERVICE] Handling payment.processed for orderId={} status={}",
                event.getOrderId(), event.getStatus());
    }

    // HANDLE PAYMENT FAILED — release reserved stock (consumed via payments.payment-failed)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("[INVENTORY-SERVICE] Handling payment.failed for orderId={}, releasing stock", event.getOrderId());
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[INVENTORY-SERVICE] No items in payment.failed event for orderId={}", event.getOrderId());
            return;
        }
        for (PaymentFailedEvent.OrderItem item : event.getItems()) {
            StockReservationRequest req = new StockReservationRequest();
            req.setQuantity(item.getQuantity());
            req.setOrderId(event.getOrderId());
            releaseStock(item.getProductId(), req);
        }
    }

    // LOG STOCK CHANGE
    private void logStockChange(Long productId, String type,
                                Integer qty, Integer before,
                                Integer after, Long orderId) {
        StockChangeLog log2 = new StockChangeLog();
        log2.setProductId(productId);
        log2.setChangeType(type);
        log2.setQuantityChanged(qty);
        log2.setStockBefore(before);
        log2.setStockAfter(after);
        log2.setOrderId(orderId);
        stockChangeLogRepository.save(log2);
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

    public BulkUpdateResponse bulkUpdateStock(BulkUpdateRequest request) {
        List<UpdateResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        for (ProductStockUpdate update : request.getUpdates()) {
            try {
                Product product = productRepository.findById(update.getProductId())
                        .orElseThrow(() ->
                                new ProductNotFoundException(PRODUCT_NOT_FOUND + ": " + update.getProductId()));
                Integer stockBefore = product.getStockQuantity();
                product.setStockQuantity(update.getNewQuantity());
                productRepository.save(product);
                int diff = update.getNewQuantity() - stockBefore;
                logStockChange(product.getId(), "BULK_UPDATE", Math.abs(diff),
                        stockBefore, update.getNewQuantity(), null);
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
