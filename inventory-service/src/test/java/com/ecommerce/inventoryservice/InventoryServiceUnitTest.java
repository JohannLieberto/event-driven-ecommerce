package com.ecommerce.inventoryservice;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.entity.StockChangeLog;
import com.ecommerce.inventoryservice.event.InventoryReservedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.repository.StockChangeLogRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceUnitTest {

    private ProductRepository productRepository;
    private StockChangeLogRepository stockChangeLogRepository;

    // ✅ FIXED TYPE
    private KafkaTemplate<String, InventoryReservedEvent> kafkaTemplate;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        stockChangeLogRepository = mock(StockChangeLogRepository.class);

        kafkaTemplate = mock(KafkaTemplate.class);

        inventoryService = new InventoryService(
                productRepository,
                stockChangeLogRepository,
                kafkaTemplate
        );
    }

    @Test
    void handleOrderCreated_withSufficientStock_publishesInventoryReserved() {

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

        // ✅ stock reduced
        assertEquals(7, product.getStockQuantity());

        // ✅ Kafka event published
        verify(kafkaTemplate).send(eq("inventory.reserved"), any(InventoryReservedEvent.class));

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

        // ✅ EXPECT EXCEPTION (your current service behavior)
        assertThrows(InsufficientStockException.class,
                () -> inventoryService.handleOrderCreated(event));

        verify(productRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void handlePaymentProcessed_confirmsReservation() {

        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId(5003L);

        assertDoesNotThrow(() -> inventoryService.handlePaymentProcessed(event));
    }

    @Test
    void handlePaymentFailed_releasesReservation() {

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

        // ✅ stock released
        assertEquals(10, product.getStockQuantity());

        verify(stockChangeLogRepository, atLeastOnce()).save(any(StockChangeLog.class));
    }
}