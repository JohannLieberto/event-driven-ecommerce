package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void order_gettersAndSetters_workCorrectly() {
        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();

        order.setId(1L);
        order.setCustomerId(100L);
        order.setStatus("PENDING");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setItems(new ArrayList<>());

        assertEquals(1L, order.getId());
        assertEquals(100L, order.getCustomerId());
        assertEquals("PENDING", order.getStatus());
        assertEquals(now, order.getCreatedAt());
        assertEquals(now, order.getUpdatedAt());
        assertNotNull(order.getItems());
    }

    @Test
    void order_defaultConstructor_initializesItems() {
        Order order = new Order();
        assertNotNull(order.getItems());
    }

    @Test
    void order_setItems_updatesItemsList() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProductId(5L);
        item.setQuantity(3);
        order.setItems(List.of(item));
        assertEquals(1, order.getItems().size());
        assertEquals(5L, order.getItems().get(0).getProductId());
    }
}
