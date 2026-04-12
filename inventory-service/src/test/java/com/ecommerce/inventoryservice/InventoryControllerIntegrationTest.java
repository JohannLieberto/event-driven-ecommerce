package com.ecommerce.inventoryservice;

import com.ecommerce.inventoryservice.entity.Product;
import com.ecommerce.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    // ✅ MOCK KAFKA (prevents context failure)
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ValidRequest_Returns201() throws Exception {
        String requestJson = """
            {
                "name": "Smartphone",
                "description": "Latest model",
                "price": 699.99,
                "stockQuantity": 100
            }
            """;

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Smartphone"))
                .andExpect(jsonPath("$.stockQuantity").value(100));
    }

    @Test
    void createProduct_InvalidPrice_Returns400() throws Exception {
        String requestJson = """
            {
                "name": "Product",
                "price": -10.00,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void getAllProducts_WithPagination_ReturnsPage() throws Exception {

        for (int i = 1; i <= 15; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setPrice(new BigDecimal("10.00"));
            product.setStockQuantity(10);
            productRepository.save(product);
        }

        mockMvc.perform(get("/api/inventory?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())           // ✅ FIXED
                .andExpect(jsonPath("$.length()").value(15)); // ✅ FIXED
    }

    @Test
    void updateProduct_ExistingProduct_Returns200() throws Exception {
        Product product = createTestProduct();

        String updateJson = """
            {
                "name": "Updated Product",
                "price": 149.99,
                "stockQuantity": 25
            }
            """;

        mockMvc.perform(put("/api/inventory/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.stockQuantity").value(25));
    }

    @Test
    void deleteProduct_ExistingProduct_Returns204() throws Exception {
        Product product = createTestProduct();

        mockMvc.perform(delete("/api/inventory/" + product.getId()))
                .andExpect(status().isNoContent());

        assertFalse(productRepository.existsById(product.getId()));
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(50);
        return productRepository.save(product);
    }
}