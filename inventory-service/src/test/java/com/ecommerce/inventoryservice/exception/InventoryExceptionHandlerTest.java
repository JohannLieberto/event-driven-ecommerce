package com.ecommerce.inventoryservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryExceptionHandlerTest {

    private InventoryExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new InventoryExceptionHandler();
    }

    @Test
    void handleProductNotFound_returns404() {
        ProductNotFoundException ex = new ProductNotFoundException("Product not found");
        ResponseEntity<Object> response = handler.handleProductNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Product not found", body.get("message"));
    }

    @Test
    void handleValidationExceptions_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("product", "name", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<Object> response = handler.handleValidationExceptions(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<Object> response = handler.handleGenericException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Unexpected error", body.get("message"));
    }
}
