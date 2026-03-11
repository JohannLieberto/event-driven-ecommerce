package com.ecommerce.orderservice.exception;

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

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleOrderNotFound_returns404() {
        OrderNotFoundException ex = new OrderNotFoundException("Order 1 not found");
        ResponseEntity<Map<String, Object>> response = handler.handleOrderNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order 1 not found", response.getBody().get("message"));
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void handleValidationException_withFieldError_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be null");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("must not be null", response.getBody().get("message"));
    }

    @Test
    void handleValidationException_withNullFieldError_returns400WithFallback() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @Test
    void handleInsufficientStock_returns409() {
        InsufficientStockException ex = new InsufficientStockException("Not enough stock");
        ResponseEntity<Map<String, Object>> response = handler.handleInsufficientStock(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Not enough stock", response.getBody().get("message"));
    }

    @Test
    void handleInventoryServiceError_returns503() {
        InventoryServiceException ex = new InventoryServiceException("Inventory down");
        ResponseEntity<Map<String, Object>> response = handler.handleInventoryServiceError(ex);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Inventory down", response.getBody().get("message"));
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new Exception("Unexpected");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
