
package com.ecommerce.orderservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createOrder_ValidRequest_Returns201() throws Exception {
        String requestJson = """
            {
                "customerId": "CUST001",
                "items": [
                    {
                        "productId": 101,
                        "quantity": 2
                    }
                ]
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.customerId").value("CUST001"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].productId").value(101))
            .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void createOrder_MissingCustomerId_Returns400() throws Exception {
        String requestJson = """
            {
                "items": [
                    {
                        "productId": 101,
                        "quantity": 2
                    }
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
        // First create an order
        String createJson = """
            {
                "customerId": "CUST002",
                "items": [
                    {
                        "productId": 102,
                        "quantity": 1
                    }
                ]
            }
            """;

        String responseBody = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract order ID (simple approach for test)
        Long orderId = 1L;

        // Now get the order
        mockMvc.perform(get("/api/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.customerId").value("CUST002"));
    }

    @Test
    void getOrder_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/api/orders/9999"))
            .andExpect(status().isNotFound());
    }
}
