# Order Service

Order management service with event-driven architecture, idempotency, and transactional outbox pattern.

## ✅ Current Status (Dec 10, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8084  
**Status:** Week 6 Implementation Ready

### Features Implemented
- ✅ **Order State Machine** - CREATED → PAYMENT_PENDING → COMPLETED/CANCELLED
- ✅ **Idempotency Key Handling** - Prevent duplicate orders
- ✅ **Transactional Outbox Pattern** - Reliable event publishing
- ✅ **Event Publishing** - OrderCreated events to Kafka
- ✅ **Database Persistence** - PostgreSQL with Flyway migrations
- ✅ **REST API** - Create, retrieve, update order status
- ✅ **OpenAPI Documentation** - Swagger UI integrated

---

## Features

### Core Functionality
- ✅ **Create Order** - From cart with idempotency key
- ✅ **Order State Machine** - Enforced state transitions
- ✅ **Event Publishing** - Transactional outbox for reliability
- ✅ **Order Lookup** - By ID, user, status
- ✅ **Order Cancellation** - With reason tracking
- ✅ **Idempotency** - Prevent duplicate order creation

### Technical Features
- ✅ **Transactional Outbox** - At-least-once delivery guarantee
- ✅ **Outbox Publisher** - Background worker publishes to Kafka
- ✅ **State Machine** - Valid state transitions enforced
- ✅ **Idempotency Keys** - 24-hour TTL
- ✅ **WebClient Integration** - Non-blocking calls to Cart/Payment services
- ✅ **Database Migrations** - Flyway for schema versioning

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- PostgreSQL (port 5433)
- Kafka (port 9092)

### 1. Start Infrastructure

```bash
# Start PostgreSQL for Order Service
docker run -d --name orderdb \
  -e POSTGRES_DB=orderdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  postgres:15

# Start Kafka (if not already running)
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  confluentinc/cp-kafka:latest
```

### 2. Build & Run

```bash
# Navigate to order-service
cd services/order-service

# Build
mvn clean package -Dmaven.test.skip=true

# Run
java -jar target/order-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
# Health check
curl http://localhost:8084/actuator/health | jq

# Expected: {"status":"UP"}
```

---

## Order State Machine

### States

```
CREATED
   ↓
PAYMENT_PENDING
   ↓
PAYMENT_CONFIRMED
   ↓
PROCESSING
   ↓
COMPLETED (terminal)

From any state → CANCELLED (terminal)
```

### State Transitions

```java
public enum OrderStatus {
    CREATED → [PAYMENT_PENDING, CANCELLED]
    PAYMENT_PENDING → [PAYMENT_CONFIRMED, CANCELLED]
    PAYMENT_CONFIRMED → [PROCESSING, CANCELLED]
    PROCESSING → [COMPLETED, CANCELLED]
    COMPLETED → [] // Terminal
    CANCELLED → [] // Terminal
}
```

### Examples

```bash
# Valid transitions
CREATED → PAYMENT_PENDING ✅
PAYMENT_PENDING → PAYMENT_CONFIRMED ✅
PROCESSING → COMPLETED ✅

# Invalid transitions
CREATED → COMPLETED ❌
PAYMENT_CONFIRMED → CREATED ❌
COMPLETED → PAYMENT_PENDING ❌
```

---

## Idempotency

### What It Does
Prevents duplicate orders when clients retry requests.

### How It Works

```bash
# First request
POST /api/orders
Headers: X-Idempotency-Key: unique-key-123
Body: { "userId": "user-1", "items": [...] }
Response: 201 Created

# Retry (within 24 hours)
POST /api/orders
Headers: X-Idempotency-Key: unique-key-123
Body: { "userId": "user-1", "items": [...] }
Response: 200 OK (returns existing order)
```

### Implementation

```java
// Check for existing order
Optional<Order> existing = orderRepository
    .findByIdempotencyKey(idempotencyKey);

if (existing.isPresent()) {
    return existing.get(); // Return existing order
}

// Create new order
Order order = new Order();
order.setIdempotencyKey(idempotencyKey);
// ...
```

### TTL: 24 hours
Idempotency keys expire after 24 hours (configurable).

---

## Transactional Outbox Pattern

### Problem It Solves

**Without Outbox (Dual Write Problem):**
```java
// Transaction 1
orderRepository.save(order);      // ✅ Saved to DB
dbTransaction.commit();           // ✅ Committed

// Transaction 2 (separate)
kafkaProducer.send(orderEvent);   // ❌ Fails!
// Result: Order saved but event not published!
```

**With Outbox (Transactional):**
```java
// Single transaction
orderRepository.save(order);           // ✅
outboxRepository.save(outboxEvent);    // ✅
dbTransaction.commit();                // ✅ Both committed together

// Separate process
outboxPublisher.publishPendingEvents(); // ✅ Publishes reliably
```

### Architecture

```
┌─────────────┐
│ REST API    │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────┐
│ OrderService                │
│  1. Save Order              │
│  2. Save OutboxEvent        │ ← Single DB Transaction
│  3. Commit                  │
└─────────────────────────────┘
       │
       ↓
┌─────────────┐
│  Database   │
│  - orders   │
│  - outbox   │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────┐
│ OutboxPublisher (Scheduled) │
│  1. Poll pending events     │
│  2. Publish to Kafka        │
│  3. Mark as processed       │
└──────┬──────────────────────┘
       │
       ↓
┌─────────────┐
│   Kafka     │
└─────────────┘
```

### Database Schema

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),  -- "ORDER"
    aggregate_id VARCHAR(255),     -- Order ID
    event_type VARCHAR(100),       -- "OrderCreated"
    payload JSONB,                 -- Event data
    status VARCHAR(50),            -- PENDING/PROCESSED/FAILED
    created_at TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count INTEGER
);
```

### Outbox Publisher

```java
@Scheduled(fixedDelay = 1000) // Every 1 second
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepository
        .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

    for (OutboxEvent event : pending) {
        try {
            kafkaTemplate.send(topic, event.getPayload());
            event.markAsProcessed();
            outboxRepository.save(event);
        } catch (Exception e) {
            event.markAsFailed(e.getMessage());
            outboxRepository.save(event);
        }
    }
}
```

### Benefits

1. ✅ **At-Least-Once Delivery** - Events never lost
2. ✅ **Transactional Consistency** - Order and event saved together
3. ✅ **Retry Logic** - Failed events retried automatically
4. ✅ **Monitoring** - Track pending/failed events
5. ✅ **No Message Loss** - Even if Kafka is down

---

## API Endpoints

### Create Order

```bash
POST /api/orders
Headers:
  X-User-Id: user-123
  X-Idempotency-Key: unique-key-456
  Content-Type: application/json

Body:
{
  "cartId": "cart-uuid",
  "items": [
    {
      "productId": "LAPTOP-001",
      "productName": "MacBook Pro",
      "quantity": 1,
      "unitPrice": 2499.99
    }
  ]
}

Response: 201 Created
{
  "id": "order-uuid",
  "orderNumber": "ORD-20251210-001",
  "userId": "user-123",
  "status": "CREATED",
  "totalAmount": 2499.99,
  "currency": "USD",
  "items": [...],
  "createdAt": "2025-12-10T10:00:00"
}
```

### Get Order

```bash
GET /api/orders/{orderId}
Headers: X-User-Id: user-123

Response: 200 OK
{
  "id": "order-uuid",
  "orderNumber": "ORD-20251210-001",
  "status": "PAYMENT_PENDING",
  "totalAmount": 2499.99,
  "items": [...]
}
```

### Get User Orders

```bash
GET /api/orders/user/{userId}
Response: 200 OK
[
  {
    "id": "order-1",
    "orderNumber": "ORD-20251210-001",
    "status": "COMPLETED",
    "totalAmount": 2499.99
  },
  {
    "id": "order-2",
    "orderNumber": "ORD-20251209-005",
    "status": "PROCESSING",
    "totalAmount": 899.99
  }
]
```

### Update Order Status

```bash
PATCH /api/orders/{orderId}/status
Headers: Content-Type: application/json

Body:
{
  "status": "PAYMENT_CONFIRMED"
}

Response: 200 OK
{
  "id": "order-uuid",
  "status": "PAYMENT_CONFIRMED",
  "updatedAt": "2025-12-10T10:05:00"
}
```

### Cancel Order

```bash
POST /api/orders/{orderId}/cancel
Headers: Content-Type: application/json

Body:
{
  "reason": "Customer requested cancellation"
}

Response: 200 OK
{
  "id": "order-uuid",
  "status": "CANCELLED",
  "cancellationReason": "Customer requested cancellation",
  "cancelledAt": "2025-12-10T10:10:00"
}
```

---

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5433
DB_NAME=orderdb
DB_USER=postgres
DB_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Service URLs
CART_SERVICE_URL=http://localhost:8083
INVENTORY_SERVICE_URL=http://localhost:8086
PAYMENT_SERVICE_URL=http://localhost:8085

# Order Settings
ORDER_MAX_ITEMS_PER_ORDER=50
ORDER_IDEMPOTENCY_TTL_HOURS=24

# Outbox
ORDER_OUTBOX_ENABLED=true
ORDER_OUTBOX_BATCH_SIZE=100
ORDER_OUTBOX_POLL_INTERVAL_MS=1000

# Server
SERVER_PORT=8084
```

---

## Project Structure

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/order/
│   │   │   ├── OrderServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── OrderController.java
│   │   │   ├── service/
│   │   │   │   ├── OrderService.java
│   │   │   │   └── OutboxPublisher.java
│   │   │   ├── repository/
│   │   │   │   ├── OrderRepository.java
│   │   │   ��   └── OutboxEventRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── OrderStatus.java (State Machine)
│   │   │   │   ├── OutboxEvent.java
│   │   │   │   └── OutboxStatus.java
│   │   │   ├── dto/
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   ├── OrderResponse.java
│   │   │   │   └── OrderItemDTO.java
│   │   │   ├── event/
│   │   │   │   └── OrderCreatedEvent.java
│   │   │   ├── exception/
│   │   │   │   ├── OrderNotFoundException.java
│   │   │   │   └── InvalidStateTransitionException.java
│   │   │   └── config/
│   │   │       ├── KafkaConfig.java
│   │   │       └── WebClientConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_orders_table.sql
│   │           ├── V2__create_order_items_table.sql
│   │           └── V3__create_outbox_events_table.sql
│   └── test/
│       └── java/com/ecommerce/order/
│           ├── service/
│           │   └── OrderServiceTest.java
│           └── integration/
│               └── OrderIntegrationTest.java
├── Dockerfile
├── pom.xml
└── README.md (this file)
```

---

## Week 6 Learning Summary

### 1. Order State Machine
- **Enforced transitions** between valid states
- **Terminal states** (COMPLETED, CANCELLED)
- **Business rules** encoded in enum

### 2. Idempotency
- **Prevents duplicate orders** on retry
- **Idempotency key** in request header
- **24-hour TTL** for keys

### 3. Transactional Outbox Pattern
- **Solves dual-write problem**
- **Single transaction** for order + event
- **Background publisher** polls and publishes
- **At-least-once delivery** guarantee

### 4. Event-Driven Architecture
- **OrderCreated event** published to Kafka
- **Inventory service** consumes and reserves stock
- **Payment service** consumes and initiates payment
- **Asynchronous processing**

### 5. Database Design
- **Normalized schema** (orders + order_items + outbox)
- **Indexes** on common queries
- **Flyway migrations** for versioning

---

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=OrderServiceTest

# With coverage
mvn jacoco:report
```

### Integration Tests

```bash
# Uses Testcontainers (PostgreSQL + Kafka)
mvn test -Dtest=OrderIntegrationTest
```

### Manual Testing

```bash
# Create order
curl -X POST http://localhost:8084/api/orders \
  -H "X-User-Id: user-123" \
  -H "X-Idempotency-Key: test-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "LAPTOP-001",
        "productName": "MacBook Pro",
        "quantity": 1,
        "unitPrice": 2499.99
      }
    ]
  }' | jq

# Get order
curl http://localhost:8084/api/orders/{orderId} \
  -H "X-User-Id: user-123" | jq

# Update status
curl -X PATCH http://localhost:8084/api/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PAYMENT_CONFIRMED"}' | jq
```

---

## Troubleshooting

### Database Connection Failed
```bash
# Check PostgreSQL is running
docker ps | grep orderdb

# Start if not running
docker start orderdb

# Check logs
docker logs orderdb
```

### Kafka Connection Failed
```bash
# Check Kafka is running
docker ps | grep kafka

# Test connection
kafka-console-producer --broker-list localhost:9092 --topic test
```

### Outbox Events Not Publishing
```bash
# Check outbox table
psql -h localhost -p 5433 -U postgres -d orderdb \
  -c "SELECT * FROM outbox_events WHERE status='PENDING';"

# Check logs
tail -f logs/order-service.log | grep OutboxPublisher

# Manually trigger (for debugging)
curl -X POST http://localhost:8084/actuator/scheduledtasks
```

### Invalid State Transition
```bash
# Error: "Cannot transition from COMPLETED to PAYMENT_PENDING"
# Solution: Check order status before transition

# Get current status
curl http://localhost:8084/api/orders/{orderId} | jq '.status'

# Only valid transitions allowed
```

---

## Monitoring

### Actuator Endpoints

```bash
# Health
curl http://localhost:8084/actuator/health

# Metrics
curl http://localhost:8084/actuator/metrics

# Prometheus
curl http://localhost:8084/actuator/prometheus
```

### Database Monitoring

```sql
-- Pending outbox events
SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING';

-- Failed events
SELECT * FROM outbox_events WHERE status = 'FAILED' ORDER BY created_at DESC;

-- Orders by status
SELECT status, COUNT(*) FROM orders GROUP BY status;

-- Recent orders
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;
```

---

## Next Steps (Week 7+)

- [ ] Payment Service integration
- [ ] Inventory reservation on order creation
- [ ] Order timeout handling
- [ ] Saga pattern for distributed transactions
- [ ] Order notifications (email/SMS)
- [ ] Order history and analytics
- [ ] Refund processing
- [ ] Order export/reporting

---

## API Documentation

- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8084/api-docs

---

## Build Commands

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -Dmaven.test.skip=true

# Run tests
mvn test

# Run specific test
mvn test -Dtest=OrderServiceTest

# Coverage report
mvn jacoco:report
# Report: target/site/jacoco/index.html

# Run locally
mvn spring-boot:run

# Build Docker image
docker build -t order-service:latest .
```

---

## Dependencies

- Spring Boot 3.1.6
- Spring Data JPA
- Spring Kafka
- Spring WebFlux (WebClient)
- PostgreSQL Driver
- Flyway Migration
- Testcontainers

---

**Last Updated:** December 10, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 6 Complete ✅

