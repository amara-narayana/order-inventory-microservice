package com.microservice.orderinventory.service;

import com.microservice.orderinventory.exception.InsufficientInventoryException;
import com.microservice.orderinventory.exception.ProductNotFoundException;
import com.microservice.orderinventory.model.Order;
import com.microservice.orderinventory.model.Product;
import com.microservice.orderinventory.repository.OrderRepository;
import com.microservice.orderinventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    // Monitor for synchronized block concurrency control
    private final Object inventoryLock = new Object();
    
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }
    
    /**
     * Creates an order with pessimistic locking to handle concurrent orders.
     * Uses synchronized blocks for additional thread safety during inventory updates.
     * Implements retry logic for network timeouts and transient failures.
     */
    @Transactional
    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Order createOrder(Long productId, Integer quantity, java.math.BigDecimal totalPrice) {
        
        logger.info("Creating order for product {} with quantity {}", productId, quantity);
        
        // Use pessimistic locking to prevent race conditions
        Product product = productRepository.findByIdWithPessimisticLock(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        // Synchronized block for additional thread safety during inventory check and update
        synchronized (inventoryLock) {
            if (product.getQuantity() < quantity) {
                throw new InsufficientInventoryException(
                    productId, quantity, product.getQuantity());
            }
            
            // Update inventory
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);
            
            logger.info("Inventory updated for product {}. New quantity: {}", 
                productId, product.getQuantity());
        }
        
        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully: {}", savedOrder.getOrderNumber());
        
        return savedOrder;
    }
    
    /**
     * Alternative method using optimistic locking with retry for concurrent updates.
     */
    @Transactional
    @Retryable(
        value = {org.springframework.orm.ObjectOptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 1.5)
    )
    public Order createOrderWithOptimisticLocking(Long productId, Integer quantity, 
                                                   java.math.BigDecimal totalPrice) {
        
        logger.info("Creating order with optimistic locking for product {} with quantity {}", 
            productId, quantity);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        if (product.getQuantity() < quantity) {
            throw new InsufficientInventoryException(
                productId, quantity, product.getQuantity());
        }
        
        // Optimistic locking will detect concurrent modifications via @Version field
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
        
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        
        return orderRepository.save(order);
    }
    
    /**
     * Retrieves an order by its order number with retry for transient failures.
     */
    @Retryable(
        value = {org.springframework.dao.DataAccessResourceFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200)
    )
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Order not found with number: " + orderNumber));
    }
    
    /**
     * Cancels an order and restores inventory.
     */
    @Transactional
    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Order cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Order not found with number: " + orderNumber));
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED || 
            order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalArgumentException(
                "Cannot cancel order with status: " + order.getStatus());
        }
        
        // Restore inventory with pessimistic locking
        Product product = productRepository.findByIdWithPessimisticLock(order.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(order.getProductId()));
        
        synchronized (inventoryLock) {
            product.setQuantity(product.getQuantity() + order.getQuantity());
            productRepository.save(product);
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
    
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
