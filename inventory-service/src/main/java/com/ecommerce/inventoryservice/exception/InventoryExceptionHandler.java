package com.ecommerce.inventoryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class InventoryExceptionHandler {

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS    = "status";
    private static final String KEY_ERROR     = "error";
    private static final String KEY_MESSAGE   = "message";

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFound(ProductNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(KEY_TIMESTAMP, LocalDateTime.now());
        error.put(KEY_STATUS, HttpStatus.NOT_FOUND.value());
        error.put(KEY_ERROR, "Not Found");
        error.put(KEY_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // InsufficientStockException must be caught explicitly — otherwise falls through
    // to the generic Exception handler which incorrectly returns 500.
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> handleInsufficientStock(InsufficientStockException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(KEY_TIMESTAMP, LocalDateTime.now());
        error.put(KEY_STATUS, HttpStatus.CONFLICT.value());
        error.put(KEY_ERROR, "Conflict");
        error.put(KEY_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((FieldError fieldError) ->
                validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_TIMESTAMP, LocalDateTime.now());
        response.put(KEY_STATUS, HttpStatus.BAD_REQUEST.value());
        response.put("errors", validationErrors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(KEY_TIMESTAMP, LocalDateTime.now());
        error.put(KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(KEY_ERROR, "Internal Server Error");
        error.put(KEY_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
