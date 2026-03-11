package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.repository.StockChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockChangeLogRepository stockChangeLogRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Test Product");
        sampleProduct.setDescription("A test product");
        sampleProduct.setPrice(new BigDecimal("9.99"));
        sampleProduct.setStockQuantity(100);
    }

    @Test
    void createProduct_savesAndReturnsResponse() {
        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setDescription("Desc");
        request.setPrice(new BigDecimal("5.00"));
        request.setStockQuantity(50);

        Product saved = new Product();
        saved.setId(2L);
        saved.setName("New Product");
        saved.setDescription("Desc");
        saved.setPrice(new BigDecimal("5.00"));
        saved.setStockQuantity(50);

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = inventoryService.createProduct(request);

        assertNotNull(response);
        assertEquals("New Product", response.getName());
        assertEquals(50, response.getStockQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProductById_found_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        ProductResponse response = inventoryService.getProductById(1L);
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> inventoryService.getProductById(99L));
    }

    @Test
    void updateProduct_found_updatesAndReturns() {
        ProductRequest request = new ProductRequest();
        request.setName("Updated");
        request.setDescription("Updated Desc");
        request.setPrice(new BigDecimal("15.00"));
        request.setStockQuantity(200);

        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Updated");
        updated.setDescription("Updated Desc");
        updated.setPrice(new BigDecimal("15.00"));
        updated.setStockQuantity(200);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = inventoryService.updateProduct(1L, request);
        assertEquals("Updated", response.getName());
        assertEquals(200, response.getStockQuantity());
    }

    @Test
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
                () -> inventoryService.updateProduct(99L, new ProductRequest()));
    }

    @Test
    void deleteProduct_found_deletesProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        inventoryService.deleteProduct(1L);
        verify(productRepository).delete(sampleProduct);
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> inventoryService.deleteProduct(99L));
    }

    @Test
    void checkStock_sufficientStock_returnsSufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        StockCheckResponse response = inventoryService.checkStock(1L, 50);
        assertTrue(response.isSufficient());
        assertEquals(100, response.getAvailableStock());
    }

    @Test
    void checkStock_insufficientStock_returnsNotSufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        StockCheckResponse response = inventoryService.checkStock(1L, 200);
        assertFalse(response.isSufficient());
    }

    @Test
    void checkStock_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> inventoryService.checkStock(99L, 10));
    }

    @Test
    void reserveStock_sufficientStock_reducesQuantity() {
        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(10);
        request.setOrderId(42L);

        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Test Product");
        updated.setStockQuantity(90);
        updated.setPrice(new BigDecimal("9.99"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = inventoryService.reserveStock(1L, request);
        assertEquals(90, response.getStockQuantity());
        verify(stockChangeLogRepository).save(any());
    }

    @Test
    void reserveStock_insufficientStock_throwsException() {
        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(200);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(1L, request));
    }

    @Test
    void releaseStock_increasesQuantity() {
        StockReservationRequest request = new StockReservationRequest();
        request.setQuantity(10);
        request.setOrderId(42L);

        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Test Product");
        updated.setStockQuantity(110);
        updated.setPrice(new BigDecimal("9.99"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = inventoryService.releaseStock(1L, request);
        assertEquals(110, response.getStockQuantity());
        verify(stockChangeLogRepository).save(any());
    }
}
