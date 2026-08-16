package com.microservice.orderinventory.controller;

import com.microservice.orderinventory.model.CreateOrderRequest;
import com.microservice.orderinventory.model.Order;
import com.microservice.orderinventory.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Creates a new order.
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received order creation request for product {}", request.getProductId());
        
        Order order = orderService.createOrder(
            request.getProductId(),
            request.getQuantity(),
            request.getTotalPrice()
        );
        
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
    
    /**
     * Retrieves an order by order number.
     * GET /api/orders/{orderNumber}
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        logger.info("Retrieving order: {}", orderNumber);
        
        Order order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Cancels an existing order.
     * PUT /api/orders/{orderNumber}/cancel
     */
    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String orderNumber) {
        logger.info("Cancelling order: {}", orderNumber);
        
        Order order = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Alternative endpoint demonstrating optimistic locking approach.
     * POST /api/orders/optimistic
     */
    @PostMapping("/optimistic")
    public ResponseEntity<Order> createOrderWithOptimisticLocking(
            @Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received order creation request with optimistic locking for product {}", 
            request.getProductId());
        
        Order order = orderService.createOrderWithOptimisticLocking(
            request.getProductId(),
            request.getQuantity(),
            request.getTotalPrice()
        );
        
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
}
