package com.ecommerce.inventoryservice;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.entity.StockChangeLog;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.repository.StockChangeLogRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceUnitTest {

    private ProductRepository productRepository;
    private StockChangeLogRepository stockChangeLogRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        stockChangeLogRepository = mock(StockChangeLogRepository.class);

        inventoryService = new InventoryService(
                productRepository,
                stockChangeLogRepository
        );
    }

    @Test
    void handleOrderCreated_withSufficientStock_reducesStock() {

        Product product = new Product();
        product.setId(4L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(10);

        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItemEvent item = new OrderItemEvent();
        item.setProductId(4L);
        item.setQuantity(3);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(5001L);
        event.setItems(List.of(item));

        inventoryService.handleOrderCreated(event);

        assertEquals(7, product.getStockQuantity());
        verify(productRepository).save(any(Product.class));
        verify(stockChangeLogRepository, atLeastOnce()).save(any(StockChangeLog.class));
    }

    @Test
    void handleOrderCreated_withInsufficientStock_throwsException() {

        Product product = new Product();
        product.setId(4L);
        product.setStockQuantity(2);

        when(productRepository.findById(4L)).thenReturn(Optional.of(product));

        OrderItemEvent item = new OrderItemEvent();
        item.setProductId(4L);
        item.setQuantity(5);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(5002L);
        event.setItems(List.of(item));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.handleOrderCreated(event));

        verify(productRepository, never()).save(any());
    }

    @Test
    void handlePaymentProcessed_doesNothing() {

        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId(5003L);

        assertDoesNotThrow(() -> inventoryService.handlePaymentProcessed(event));
    }

    @Test
    void handlePaymentFailed_releasesStock() {

        Product product = new Product();
        product.setId(4L);
        product.setStockQuantity(7);

        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentFailedEvent.OrderItem item = new PaymentFailedEvent.OrderItem();
        item.setProductId(4L);
        item.setQuantity(3);

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setOrderId(5004L);
        event.setItems(List.of(item));

        inventoryService.handlePaymentFailed(event);

        assertEquals(10, product.getStockQuantity());
        verify(stockChangeLogRepository, atLeastOnce()).save(any(StockChangeLog.class));
    }
}
