package com.example.inventory.exceptions;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException(String productId) {
        super("Product already exists: " + productId);
    }
}