package com.ecommerce.orderservice.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderNotFoundExceptionTest {

    @Test
    void constructor_setsMessage() {
        OrderNotFoundException ex = new OrderNotFoundException("Order not found");
        assertEquals("Order not found", ex.getMessage());
    }
}
