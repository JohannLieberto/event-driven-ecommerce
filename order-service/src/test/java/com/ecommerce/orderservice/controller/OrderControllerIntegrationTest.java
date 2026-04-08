package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:integrationdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryClient inventoryClient;

    // ✅ FINAL FIX: MOCK THE ACTUAL CLASS USED
    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setup() {
        // Mock inventory
        when(inventoryClient.checkStock(anyLong(), anyInt())).thenReturn(true);

        // ✅ Prevent Kafka call completely
        doNothing().when(orderEventPublisher).publishOrderCreated(any());
    }

    @Test
    void createOrder_ValidRequest_Returns201() throws Exception {
        String requestJson = """
        {
            "customerId": 1001,
            "items": [
                { "productId": 101, "quantity": 2 }
            ]
        }
        """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(1001));
    }

    @Test
    void createOrder_MissingCustomerId_Returns400() throws Exception {
        String requestJson = """
        {
            "items": [
                { "productId": 101, "quantity": 2 }
            ]
        }
        """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_ExistingId_Returns200() throws Exception {
        String createJson = """
        {
            "customerId": 1002,
            "items": [
                { "productId": 102, "quantity": 1 }
            ]
        }
        """;

        String responseBody = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(responseBody);
        Long orderId = jsonNode.get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.customerId").value(1002));
    }

    @Test
    void getOrder_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/api/orders/9999"))
                .andExpect(status().isNotFound());
    }
}