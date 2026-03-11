package com.ecommerce.orderservice.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceExceptionTest {

    @Test
    void constructor_setsMessage() {
        InventoryServiceException ex = new InventoryServiceException("Service error");
        assertEquals("Service error", ex.getMessage());
    }
}
