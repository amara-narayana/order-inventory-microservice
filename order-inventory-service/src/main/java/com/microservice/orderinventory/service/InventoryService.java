package com.microservice.orderinventory.service;

import com.microservice.orderinventory.model.Product;
import com.microservice.orderinventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final ProductRepository productRepository;
    
    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    /**
     * Retrieves all products with retry logic for transient database failures.
     */
    @Retryable(
        value = {org.springframework.dao.DataAccessResourceFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    /**
     * Retrieves a product by ID with pessimistic locking for update operations.
     */
    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productRepository.findByIdWithPessimisticLock(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Product not found with id: " + productId));
    }
    
    /**
     * Updates product inventory with pessimistic locking to handle concurrent updates.
     */
    @Transactional
    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Product updateInventory(Long productId, Integer quantityAdjustment) {
        logger.info("Updating inventory for product {} with adjustment {}", 
            productId, quantityAdjustment);
        
        Product product = productRepository.findByIdWithPessimisticLock(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Product not found with id: " + productId));
        
        int newQuantity = product.getQuantity() + quantityAdjustment;
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                "Cannot reduce inventory below zero. Current: " + product.getQuantity() + 
                ", Adjustment: " + quantityAdjustment);
        }
        
        product.setQuantity(newQuantity);
        Product updatedProduct = productRepository.save(product);
        
        logger.info("Inventory updated for product {}. New quantity: {}", 
            productId, updatedProduct.getQuantity());
        
        return updatedProduct;
    }
    
    /**
     * Adds a new product to inventory.
     */
    @Transactional
    public Product addProduct(String sku, String name, Integer initialQuantity) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setQuantity(initialQuantity);
        
        Product savedProduct = productRepository.save(product);
        logger.info("New product added: {} with SKU {}", savedProduct.getName(), savedProduct.getSku());
        
        return savedProduct;
    }
    
    /**
     * Checks product availability without locking (for read-only operations).
     */
    @Transactional(readOnly = true)
    public boolean isProductAvailable(Long productId, Integer requiredQuantity) {
        return productRepository.findById(productId)
            .map(product -> product.getQuantity() >= requiredQuantity)
            .orElse(false);
    }
}
