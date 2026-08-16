# Scalable Order & Inventory Microservice

A production-ready microservice built with **Java**, **Spring Boot**, and **MySQL** that demonstrates expertise in API design, concurrency handling, and resilience patterns for distributed systems.

## 🚀 Features

### API Design & Architecture
- RESTful APIs following best practices for inter-service communication
- Stateless design enabling horizontal scaling
- Comprehensive input validation using Jakarta Validation
- Global exception handling with structured error responses

### Concurrency & Reliability
- **Pessimistic Locking**: Uses `PESSIMISTIC_WRITE` locks to handle 100+ concurrent orders
- **Synchronized Blocks**: Additional thread safety during critical inventory updates
- **Optimistic Locking**: Alternative approach using JPA `@Version` for conflict detection
- Race condition prevention in distributed order processing

### Resilience Patterns
- **Spring Retry**: Automatic retry logic for transient failures
  - Database lock acquisition failures
  - Network timeouts
  - Temporary resource unavailability
- Configurable backoff strategies (delay, multiplier, max attempts)
- Reproducible failure-handling scenarios

## 📁 Project Structure

```
order-inventory-service/
├── src/main/java/com/microservice/orderinventory/
│   ├── config/
│   │   └── RetryConfig.java          # Spring Retry configuration
│   ├── controller/
│   │   ├── OrderController.java      # Order management endpoints
│   │   └── InventoryController.java  # Inventory management endpoints
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InsufficientInventoryException.java
│   │   └── ProductNotFoundException.java
│   ├── model/
│   │   ├── Order.java                # Order entity
│   │   ├── Product.java              # Product entity
│   │   └── CreateOrderRequest.java   # DTO for order creation
│   ├── repository/
│   │   ├── OrderRepository.java      # JPA repository with locking
│   │   └── ProductRepository.java    # JPA repository with locking
│   ├── service/
│   │   ├── OrderService.java         # Order business logic
│   │   └── InventoryService.java     # Inventory business logic
│   └── OrderInventoryApplication.java
├── src/main/resources/
│   ├── application.properties        # Configuration (properties format)
│   └── application.yml               # Configuration (YAML format)
├── src/test/java/
│   └── OrderServiceTest.java         # Unit tests
└── pom.xml                           # Maven dependencies
```

## 🛠️ Technologies

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **MySQL**
- **Spring Retry**
- **Jakarta Validation**
- **Maven**
- **JUnit 5 & Mockito**

## 🔧 Setup & Configuration

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

### Database Setup

```sql
CREATE DATABASE order_inventory_db;
USE order_inventory_db;
```

### Configuration

Update `src/main/resources/application.properties` or `application.yml`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_inventory_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### Build & Run

```bash
# Navigate to project directory
cd order-inventory-service

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📡 API Endpoints

### Order Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create a new order |
| GET | `/api/orders/{orderNumber}` | Get order by number |
| PUT | `/api/orders/{orderNumber}/cancel` | Cancel an order |
| POST | `/api/orders/optimistic` | Create order with optimistic locking |

### Inventory Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory/products` | Get all products |
| GET | `/api/inventory/products/{id}` | Get product by ID |
| POST | `/api/inventory/products` | Add new product |
| PUT | `/api/inventory/products/{id}/adjust` | Adjust inventory quantity |
| GET | `/api/inventory/products/{id}/availability` | Check product availability |

## 📝 Example Requests

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 5,
    "totalPrice": 99.99
  }'
```

### Add a Product

```bash
curl -X POST "http://localhost:8080/api/inventory/products?sku=LAPTOP-001&name=Gaming+Laptop&initialQuantity=100"
```

### Cancel an Order

```bash
curl -X PUT http://localhost:8080/api/orders/ORD-ABC12345/cancel
```

## 🔒 Concurrency Handling

### Pessimistic Locking Approach

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithPessimisticLock(Long id);
```

Used in `OrderService.createOrder()` to prevent race conditions during high-concurrency scenarios.

### Synchronized Blocks

```java
synchronized (inventoryLock) {
    if (product.getQuantity() < quantity) {
        throw new InsufficientInventoryException(...);
    }
    product.setQuantity(product.getQuantity() - quantity);
}
```

Provides additional thread safety during critical inventory updates.

### Retry Configuration

```java
@Retryable(
    value = {CannotAcquireLockException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
```

Automatically retries failed operations due to lock contention or transient failures.

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest
```

## 📊 Monitoring

Spring Boot Actuator endpoints are enabled:

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

## 🎯 Key Learnings Demonstrated

1. **API Design Principles**: RESTful design, proper HTTP methods, status codes, and request/response structures
2. **Stateless Horizontal Scaling**: No session state, enabling easy scaling across multiple instances
3. **Race Condition Resolution**: Multiple strategies (pessimistic/optimistic locking, synchronized blocks)
4. **Distributed Systems Reliability**: Retry patterns, timeout handling, and graceful failure recovery
5. **Database Transaction Management**: Proper use of `@Transactional` with appropriate isolation levels

## 📄 License

This project is for educational and portfolio purposes.
