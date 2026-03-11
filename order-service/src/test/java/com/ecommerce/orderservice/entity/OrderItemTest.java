package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void orderItem_gettersAndSetters_workCorrectly() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setQuantity(5);
        item.setPrice(new BigDecimal("19.99"));

        assertEquals(1L, item.getId());
        assertEquals(10L, item.getProductId());
        assertEquals(5, item.getQuantity());
        assertEquals(new BigDecimal("19.99"), item.getPrice());
    }
}
