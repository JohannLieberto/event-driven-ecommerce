package com.ecommerce.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_MESSAGE = "message";

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_TIMESTAMP, LocalDateTime.now());
        body.put(FIELD_STATUS, HttpStatus.NOT_FOUND.value());
        body.put(FIELD_ERROR, "Not Found");
        body.put(FIELD_MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(FIELD_TIMESTAMP, LocalDateTime.now());
        error.put(FIELD_STATUS, 400);
        error.put(FIELD_ERROR, "Bad Request");
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        error.put(FIELD_MESSAGE, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_TIMESTAMP, LocalDateTime.now());
        body.put(FIELD_STATUS, HttpStatus.CONFLICT.value());
        body.put(FIELD_ERROR, "Conflict");
        body.put(FIELD_MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryServiceError(InventoryServiceException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_TIMESTAMP, LocalDateTime.now());
        body.put(FIELD_STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put(FIELD_ERROR, "Service Unavailable");
        body.put(FIELD_MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_TIMESTAMP, LocalDateTime.now());
        body.put(FIELD_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put(FIELD_ERROR, "Internal Server Error");
        body.put(FIELD_MESSAGE, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
