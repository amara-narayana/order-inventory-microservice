package com.microservice.orderinventory.service;

import com.microservice.orderinventory.exception.InsufficientInventoryException;
import com.microservice.orderinventory.exception.ProductNotFoundException;
import com.microservice.orderinventory.model.Order;
import com.microservice.orderinventory.model.Product;
import com.microservice.orderinventory.repository.OrderRepository;
import com.microservice.orderinventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    private Product testProduct;
    
    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setSku("TEST-SKU-001");
        testProduct.setName("Test Product");
        testProduct.setQuantity(100);
        testProduct.setVersion(0L);
    }
    
    @Test
    void createOrder_Success() {
        // Arrange
        Long productId = 1L;
        Integer quantity = 5;
        BigDecimal totalPrice = new BigDecimal("99.99");
        
        when(productRepository.findByIdWithPessimisticLock(productId))
            .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        
        // Act
        Order result = orderService.createOrder(productId, quantity, totalPrice);
        
        // Assert
        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus().name());
        assertEquals(productId, result.getProductId());
        assertEquals(quantity, result.getQuantity());
        assertEquals(totalPrice, result.getTotalPrice());
        
        // Verify inventory was reduced
        verify(productRepository).save(argThat(p -> p.getQuantity() == 95));
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    void createOrder_ProductNotFound() {
        // Arrange
        Long productId = 999L;
        when(productRepository.findByIdWithPessimisticLock(productId))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> 
            orderService.createOrder(productId, 5, new BigDecimal("50.00")));
    }
    
    @Test
    void createOrder_InsufficientInventory() {
        // Arrange
        testProduct.setQuantity(3);
        Long productId = 1L;
        Integer quantity = 10;
        
        when(productRepository.findByIdWithPessimisticLock(productId))
            .thenReturn(Optional.of(testProduct));
        
        // Act & Assert
        assertThrows(InsufficientInventoryException.class, () -> 
            orderService.createOrder(productId, quantity, new BigDecimal("100.00")));
    }
    
    @Test
    void cancelOrder_Success() {
        // Arrange
        Order existingOrder = new Order();
        existingOrder.setId(1L);
        existingOrder.setOrderNumber("ORD-TEST123");
        existingOrder.setProductId(1L);
        existingOrder.setQuantity(5);
        existingOrder.setStatus(Order.OrderStatus.CONFIRMED);
        
        when(orderRepository.findByOrderNumber("ORD-TEST123"))
            .thenReturn(Optional.of(existingOrder));
        when(productRepository.findByIdWithPessimisticLock(1L))
            .thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setStatus(Order.OrderStatus.CANCELLED);
            return order;
        });
        
        // Act
        Order result = orderService.cancelOrder("ORD-TEST123");
        
        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.CANCELLED, result.getStatus());
        verify(productRepository).save(argThat(p -> p.getQuantity() == 105));
    }
    
    @Test
    void cancelOrder_AlreadyCancelled() {
        // Arrange
        Order cancelledOrder = new Order();
        cancelledOrder.setId(1L);
        cancelledOrder.setOrderNumber("ORD-TEST123");
        cancelledOrder.setStatus(Order.OrderStatus.CANCELLED);
        
        when(orderRepository.findByOrderNumber("ORD-TEST123"))
            .thenReturn(Optional.of(cancelledOrder));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            orderService.cancelOrder("ORD-TEST123"));
    }
}
