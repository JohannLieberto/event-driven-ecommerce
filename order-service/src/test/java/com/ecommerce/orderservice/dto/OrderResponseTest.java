package com.ecommerce.orderservice.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderResponseTest {

    @Test
    void orderResponse_gettersAndSetters_workCorrectly() {
        OrderItemResponse itemResponse = new OrderItemResponse();
        itemResponse.setId(1L);
        itemResponse.setProductId(10L);
        itemResponse.setQuantity(2);
        itemResponse.setPrice(new BigDecimal("9.99"));

        LocalDateTime now = LocalDateTime.now();
        OrderResponse response = new OrderResponse();
        response.setId(1L);
        response.setCustomerId(100L);
        response.setStatus("CONFIRMED");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setItems(List.of(itemResponse));

        assertEquals(1L, response.getId());
        assertEquals(100L, response.getCustomerId());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertEquals(1, response.getItems().size());
        assertEquals(10L, response.getItems().get(0).getProductId());
        assertEquals(new BigDecimal("9.99"), response.getItems().get(0).getPrice());
    }
}
