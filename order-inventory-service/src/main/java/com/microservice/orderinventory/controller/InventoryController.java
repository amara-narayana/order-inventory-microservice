package com.microservice.orderinventory.controller;

import com.microservice.orderinventory.model.Product;
import com.microservice.orderinventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Retrieves all products in inventory.
     * GET /api/inventory/products
     */
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Retrieving all products");
        List<Product> products = inventoryService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Retrieves a product by ID.
     * GET /api/inventory/products/{id}
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.info("Retrieving product with id: {}", id);
        Product product = inventoryService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Updates product inventory (positive or negative adjustment).
     * PUT /api/inventory/products/{id}/adjust
     */
    @PutMapping("/products/{id}/adjust")
    public ResponseEntity<Product> updateInventory(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        logger.info("Adjusting inventory for product {} by {}", id, quantity);
        Product product = inventoryService.updateInventory(id, quantity);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Adds a new product to inventory.
     * POST /api/inventory/products
     */
    @PostMapping("/products")
    public ResponseEntity<Product> addProduct(
            @RequestParam String sku,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") Integer initialQuantity) {
        logger.info("Adding new product: {} with SKU {}", name, sku);
        Product product = inventoryService.addProduct(sku, name, initialQuantity);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Checks if a product is available in required quantity.
     * GET /api/inventory/products/{id}/availability?quantity={qty}
     */
    @GetMapping("/products/{id}/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer quantity) {
        logger.info("Checking availability for product {} with quantity {}", id, quantity);
        boolean available = inventoryService.isProductAvailable(id, quantity);
        return ResponseEntity.ok(available);
    }
}
