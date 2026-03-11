package com.ecommerce.orderservice.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsufficientStockExceptionTest {

    @Test
    void constructor_setsMessage() {
        InsufficientStockException ex = new InsufficientStockException("Insufficient stock");
        assertEquals("Insufficient stock", ex.getMessage());
    }
}
