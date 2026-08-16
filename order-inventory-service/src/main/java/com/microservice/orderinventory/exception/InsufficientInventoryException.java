package com.microservice.orderinventory.exception;

public class InsufficientInventoryException extends RuntimeException {
    
    public InsufficientInventoryException(String message) {
        super(message);
    }
    
    public InsufficientInventoryException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super(String.format("Insufficient inventory for product %d. Requested: %d, Available: %d", 
                productId, requestedQuantity, availableQuantity));
    }
}
