package com.ecommerce.inventoryservice;

import com.ecommerce.inventoryservice.dto.ProductRequest;
import com.ecommerce.inventoryservice.dto.ProductResponse;
import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false"
        }
)
@ActiveProfiles("test")
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ValidRequest_Success() {

        ProductRequest request = new ProductRequest();
        request.setName("Laptop");
        request.setDescription("High performance laptop");
        request.setPrice(new BigDecimal("999.99"));
        request.setStockQuantity(50);

        ProductResponse response = inventoryService.createProduct(request);

        assertNotNull(response.getId());
        assertEquals("Laptop", response.getName());
        assertEquals(50, response.getStockQuantity());
    }

    @Test
    void getProductById_ExistingProduct_Success() {

        Product product = createTestProduct();

        ProductResponse response =
                inventoryService.getProductById(product.getId());

        assertEquals(product.getId(), response.getId());
        assertEquals(product.getName(), response.getName());
    }

    @Test
    void getProductById_NonExistentProduct_ThrowsException() {

        assertThrows(
                ProductNotFoundException.class,
                () -> inventoryService.getProductById(999L)
        );
    }

    @Test
    void updateProduct_ExistingProduct_Success() {

        Product product = createTestProduct();

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Updated Product");
        updateRequest.setPrice(new BigDecimal("1299.99"));
        updateRequest.setStockQuantity(75);

        ProductResponse response =
                inventoryService.updateProduct(product.getId(), updateRequest);

        assertEquals("Updated Product", response.getName());
        assertEquals(75, response.getStockQuantity());
    }

    @Test
    void deleteProduct_ExistingProduct_Success() {

        Product product = createTestProduct();

        inventoryService.deleteProduct(product.getId());

        assertFalse(productRepository.existsById(product.getId()));
    }

    private Product createTestProduct() {

        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(10);

        return productRepository.save(product);
    }
}
