package com.microservice.orderinventory.repository;

import com.microservice.orderinventory.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findByProductId(Long productId, Pageable pageable);
}
