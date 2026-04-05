package com.ecommerce.inventoryservice.exception;

public class StockConcurrencyException extends RuntimeException {

    public StockConcurrencyException(String message) {
        super(message);
    }

}