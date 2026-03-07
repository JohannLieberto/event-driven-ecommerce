package com.ecommerce.inventoryservice;

import com.ecommerce.inventoryservice.dto.ProductResponse;
import com.ecommerce.inventoryservice.dto.StockReservationRequest;
import com.ecommerce.inventoryservice.entity.Product;
import org.springframework.test.context.ActiveProfiles;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.service.InventoryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StockReservationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void reserveStock_SufficientStock_Success() {

        Product product = createProductWithStock(50);

        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(10);
        request.setOrderId(123L);

        ProductResponse response =
                inventoryService.reserveStock(product.getId(), request);

        assertEquals(40, response.getStockQuantity());
    }

    @Test
    void reserveStock_InsufficientStock_ThrowsException() {

        Product product = createProductWithStock(5);

        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(10);

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.reserveStock(product.getId(), request)
        );
    }

    @Test
    void releaseStock_Success() {

        Product product = createProductWithStock(40);

        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(10);
        request.setOrderId(123L);

        ProductResponse response =
                inventoryService.releaseStock(product.getId(), request);

        assertEquals(50, response.getStockQuantity());
    }

    @Test
    void reserveStock_ConcurrentRequests_OneSucceeds() throws Exception {

        Product product = createProductWithStock(10);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable reserveTask = () -> {
            try {

                StockReservationRequest request =
                        new StockReservationRequest();

                request.setQuantity(6);

                inventoryService.reserveStock(product.getId(), request);

                successCount.incrementAndGet();

            } catch (Exception e) {

                failureCount.incrementAndGet();

            } finally {

                latch.countDown();
            }
        };

        executor.submit(reserveTask);
        executor.submit(reserveTask);

        latch.await(10, TimeUnit.SECONDS);

        executor.shutdown();

        // Only one request should succeed
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        // Final stock should be 4 (10 - 6)
        Product updated =
                productRepository.findById(product.getId()).get();

        assertEquals(4, updated.getStockQuantity());
    }

    private Product createProductWithStock(int stock) {

        Product product = new Product();

        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(stock);

        return productRepository.save(product);
    }
}