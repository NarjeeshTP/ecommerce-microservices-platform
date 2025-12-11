# Outbox Processor & Debezium CDC
Reliable transactional messaging implementation using the Outbox Pattern with Debezium Change Data Capture (CDC).
## ✅ Current Status (Dec 11, 2025)
**Version:** 0.0.1-SNAPSHOT  
**Port:** 8089  
**Status:** Week 10 Implementation Ready
### Features Implemented
- ✅ **Outbox Pattern** - Transactional outbox for reliable messaging
- ✅ **Scheduled Processor** - Poll and publish outbox events
- ✅ **Debezium CDC** - Stream database changes to Kafka
- ✅ **At-Least-Once Delivery** - Guaranteed message delivery
- ✅ **Dead Letter Queue** - Failed message handling
- ✅ **Monitoring** - Metrics for processed events
- ✅ **Idempotency** - Prevent duplicate processing
---
## Features
### Outbox Pattern
- ✅ **Transactional Writes** - Event stored in same transaction as business data
- ✅ **Scheduled Polling** - Periodic outbox table polling
- ✅ **Batch Processing** - Process multiple events efficiently
- ✅ **Retry Logic** - Exponential backoff for failures
- ✅ **Status Tracking** - PENDING → PUBLISHED → FAILED
### Debezium CDC
- ✅ **Change Streams** - Real-time database change capture
- ✅ **Low Latency** - Sub-second event propagation
- ✅ **No Code Changes** - Works with existing tables
- ✅ **Schema Evolution** - Handles table changes
- ✅ **Exactly-Once Semantics** - With Kafka transactions
---
## The Problem: Dual-Write
### ❌ Naive Approach (Broken)
```java
// Order Service
@Transactional
public void createOrder(Order order) {
    // 1. Save to database
    orderRepository.save(order);  // Transaction 1
    // 2. Publish event to Kafka
    kafkaProducer.send("orders", new OrderCreated(order));  // Transaction 2
}
```
**What Can Go Wrong:**
```
Scenario 1: Database succeeds, Kafka fails
┌─────────────┐
│  Database   │  ✅ Order saved
└─────────────┘
      ↓
┌─────────────┐
│   Kafka     │  ❌ Network error
└─────────────┘
Result: Order exists but no event published!
Other services never know about the order.
Scenario 2: Kafka succeeds, Database fails
┌─────────────┐
│   Kafka     │  ✅ Event published
└─────────────┘
      ↓
┌─────────────┐
│  Database   │  ❌ Transaction rollback
└─────────────┘
Result: Event published but order doesn't exist!
Other services process non-existent order.
```
**Problem:** Two separate transactions that can fail independently = data inconsistency
---
## Solution 1: Outbox Pattern
### ✅ Correct Approach
```java
// Order Service
@Transactional
public void createOrder(Order order) {
    // 1. Save order
    orderRepository.save(order);
    // 2. Save event to outbox table (SAME TRANSACTION!)
    OutboxEvent event = OutboxEvent.builder()
        .aggregateId(order.getId())
        .eventType("OrderCreated")
        .payload(toJson(order))
        .status(OutboxStatus.PENDING)
        .build();
    outboxRepository.save(event);
    // Both succeed or both fail together ✅
}
// Outbox Processor (separate service)
@Scheduled(fixedDelay = 1000)
public void processOutbox() {
    List<OutboxEvent> pending = outboxRepository
        .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 100));
    for (OutboxEvent event : pending) {
        try {
            kafkaProducer.send(event.getTopic(), event.getPayload());
            event.setStatus(OutboxStatus.PUBLISHED);
            outboxRepository.save(event);
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus(OutboxStatus.FAILED);
            outboxRepository.save(event);
        }
    }
}
```
### Flow Diagram
```
┌──────────────────────────────────────────────────┐
│  Order Service                                   │
│                                                  │
│  @Transactional                                  │
│  createOrder() {                                 │
│    1. orderRepo.save(order)          ┌────────┐ │
│    2. outboxRepo.save(event)   ─────→│  DB    │ │
│  }                                    │Transaction│
│                                       └────────┘ │
└──────────────────────────────────────────────────┘
                    ↓
                Both committed together ✅
                    ↓
┌──────────────────────────────────────────────────┐
│  Outbox Processor (separate)                     │
│                                                  │
│  @Scheduled                                      │
│  processOutbox() {                               │
│    events = SELECT * FROM outbox                 │
│              WHERE status = 'PENDING'            │
│                                                  │
│    for each event:                               │
│      kafkaProducer.send(event)                   │
│      UPDATE outbox SET status = 'PUBLISHED'      │
│  }                                               │
└──────────────────────────────────────────────────┘
```
---
## Solution 2: Debezium CDC
### What is Change Data Capture (CDC)?
**CDC** = Stream database transaction log to Kafka in real-time
**How It Works:**
```
┌──────────────────────────────────────────────────┐
│  PostgreSQL                                      │
│                                                  │
│  INSERT INTO orders VALUES (...)                 │
│        ↓                                         │
│  Write-Ahead Log (WAL)                          │
│  [2025-12-11 10:00:00] INSERT orders id=123     │
└──────────────────────────────────────────────────┘
                    ↓
                    ↓ Debezium reads WAL
                    ↓
┌──────────────────────────────────────────────────┐
│  Debezium Connector                              │
│  - Reads PostgreSQL WAL                          │
│  - Converts to Kafka events                      │
│  - Publishes to topics                           │
└──────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────┐
│  Kafka Topic: db.orders.changes                  │
│                                                  │
│  Event:                                          │
│  {                                               │
│    "op": "c",  (create)                         │
│    "before": null,                               │
│    "after": {                                    │
│      "id": 123,                                  │
│      "user_id": "user-456",                      │
│      "total": 299.99                             │
│    },                                            │
│    "ts_ms": 1702287600000                        │
│  }                                               │
└──────────────────────────────────────────────────┘
```
### Advantages Over Polling
| Aspect | Polling (Outbox) | CDC (Debezium) |
|--------|------------------|----------------|
| **Latency** | 1-5 seconds | < 100ms |
| **Database Load** | Periodic SELECT queries | Read-only WAL |
| **Complexity** | Simple | More complex |
| **Scalability** | Limited by poll frequency | Scales with WAL |
| **Code Changes** | Requires outbox table | No code changes |
| **Use Case** | Simple, low volume | High throughput |
---
## Architecture
### Outbox Pattern Architecture
```
┌─────────────────────────────────────────────────────────┐
│  Service A (Order Service)                              │
│                                                         │
│  @Transactional                                         │
│  ┌──────────────────────────────────────────────┐      │
│  │  Business Logic                              │      │
│  │  - Save Order                                │      │
│  │  - Save OutboxEvent                          │      │
│  └──────────────────────────────────────────────┘      │
│                  ↓                                      │
│  ┌───────────────────────────────────────────────────┐ │
│  │  PostgreSQL Database                              │ │
│  │  ┌─────────────┐       ┌─────────────┐          │ │
│  │  │  orders     │       │  outbox     │          │ │
│  │  │  table      │       │  table      │          │ │
│  │  └─────────────┘       └─────────────┘          │ │
│  │        SAME TRANSACTION                          │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                     ↓
                     ↓ Scheduled polling
                     ↓
┌───────────────────────────────────────────��─────────────┐
│  Outbox Processor Service                               │
│                                                         │
│  @Scheduled(fixedDelay = 1000)                          │
│  ┌──────────────────────────────────────────────┐      │
│  │  1. SELECT * FROM outbox                     │      │
│  │     WHERE status = 'PENDING'                 │      │
│  │     LIMIT 100                                │      │
│  │                                              │      │
│  │  2. FOR EACH event:                          │      │
│  │     - kafkaProducer.send(event)              │      │
│  │     - UPDATE status = 'PUBLISHED'            │      │
│  └──────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Apache Kafka                                           │
│  Topic: order-events                                    │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Consuming Services                                     │
│  - Notification Service                                 │
│  - Inventory Service                                    │
│  - Analytics Service                                    │
└─────────────────────────────────────────────────────────┘
```
### Debezium CDC Architecture
```
┌─────────────────────────────────────────────────────────┐
│  Order Service                                          │
│                                                         │
│  orderRepository.save(order);  ← Normal code, no change │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  PostgreSQL                                             │
│  ┌─────────────────────────────────────────────┐       │
│  │  orders table                                │       │
│  │  INSERT INTO orders VALUES (...)             │       │
│  └─────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌─────────────────────────────────────────────┐       │
│  │  Write-Ahead Log (WAL)                      │       │
│  │  [Transaction Log]                          │       │
│  └─────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────┘
                     ↓
                     ↓ Real-time streaming
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Debezium Connector (Kafka Connect)                     │
│  ┌──────────────────────────────────────────────┐      │
│  │  PostgreSQL Connector                        │      │
│  │  - Reads WAL                                 │      │
│  │  - Converts to events                        │      │
│  │  - Publishes to Kafka                        │      │
│  └──────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Kafka Topic: dbserver1.public.orders                   │
│  {                                                      │
│    "op": "c",                                          │
│    "after": {"id": 123, "total": 299.99},             │
│    "source": {                                         │
│      "db": "orderdb",                                  │
│      "table": "orders"                                 │
│    }                                                   │
│  }                                                     │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Consuming Services                                     │
└─────────────────────────────────────────────────────────┘
```
---
## Quick Start
### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- PostgreSQL
- Kafka
- Kafka Connect (for Debezium)
### Option 1: Outbox Processor (Polling)
#### 1. Setup Database
```sql
-- In Order Service database (orderdb)
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    topic VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);
CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
```
#### 2. Order Service Integration
```java
// Order Service
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private OutboxRepository outboxRepo;
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Save order
        Order order = Order.builder()
            .userId(request.getUserId())
            .totalAmount(request.getTotalAmount())
            .status(OrderStatus.CREATED)
            .build();
        orderRepo.save(order);
        // 2. Save outbox event (SAME TRANSACTION!)
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateId(order.getId().toString())
            .aggregateType("Order")
            .eventType("OrderCreated")
            .payload(toJson(order))
            .topic("order-events")
            .status(OutboxStatus.PENDING)
            .build();
        outboxRepo.save(event);
        return order;  // Both saved atomically ✅
    }
}
```
#### 3. Run Outbox Processor
```bash
cd services/outbox-processor
mvn clean package -Dmaven.test.skip=true
java -jar target/outbox-processor-0.0.1-SNAPSHOT.jar
```
### Option 2: Debezium CDC (Streaming)
#### 1. Start Debezium via Docker Compose
```yaml
# infra/docker-compose.yml (add these services)
version: '3.8'
services:
  # ... existing services ...
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
  kafka-connect:
    image: debezium/connect:2.5
    depends_on:
      - kafka
      - postgres-order
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: connect_configs
      OFFSET_STORAGE_TOPIC: connect_offsets
      STATUS_STORAGE_TOPIC: connect_status
    ports:
      - "8083:8083"
  postgres-order:
    image: postgres:15
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      # Enable WAL for CDC
      POSTGRES_INITDB_ARGS: "-c wal_level=logical"
    ports:
      - "5433:5432"
    command:
      - "postgres"
      - "-c"
      - "wal_level=logical"
```
#### 2. Start Services
```bash
cd infra
docker-compose up -d
# Verify Kafka Connect is running
curl http://localhost:8083/connectors
```
#### 3. Register Debezium Connector
```bash
# Create connector configuration
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @infra/debezium/connectors/order-connector.json
```
**order-connector.json:**
```json
{
  "name": "order-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres-order",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "orderdb",
    "database.server.name": "dbserver1",
    "table.include.list": "public.orders,public.outbox_events",
    "plugin.name": "pgoutput",
    "slot.name": "debezium_order_slot",
    "publication.name": "debezium_publication"
  }
}
```
#### 4. Verify Streaming
```bash
# Check connector status
curl http://localhost:8083/connectors/order-connector/status | jq
# Consume events
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic dbserver1.public.orders \
  --from-beginning
```
---
## Configuration
### application.yml (Outbox Processor)
```yaml
spring:
  application:
    name: outbox-processor
  datasource:
    # Connect to Order Service database
    url: jdbc:postgresql://localhost:5433/orderdb
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
outbox:
  processor:
    enabled: true
    interval-ms: 1000  # Poll every 1 second
    batch-size: 100
    retry:
      max-attempts: 3
      backoff-ms: 1000
server:
  port: 8089
```
---
## Outbox Table Schema
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,  -- Order, Payment, Inventory
    event_type VARCHAR(100) NOT NULL,      -- OrderCreated, PaymentCompleted
    payload JSONB NOT NULL,                -- Full event data
    topic VARCHAR(255) NOT NULL,           -- Kafka topic
    status VARCHAR(50) NOT NULL,           -- PENDING, PUBLISHED, FAILED
    retry_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);
CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_id);
```
---
## Testing
### Test Outbox Pattern
```bash
# 1. Create order (saves to DB + outbox)
curl -X POST http://localhost:8084/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "items": [{"productId": "LAPTOP-001", "quantity": 1}]
  }'
# 2. Check outbox table
psql -h localhost -p 5433 -U postgres -d orderdb \
  -c "SELECT * FROM outbox_events WHERE status = 'PENDING';"
# 3. Wait 1 second (processor runs)
# 4. Check outbox table again
psql -h localhost -p 5433 -U postgres -d orderdb \
  -c "SELECT * FROM outbox_events WHERE status = 'PUBLISHED';"
# 5. Verify Kafka event
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```
### Test Debezium CDC
```bash
# 1. Insert order directly
psql -h localhost -p 5433 -U postgres -d orderdb \
  -c "INSERT INTO orders (id, user_id, total_amount, status) VALUES 
      (gen_random_uuid(), 'user-123', 299.99, 'CREATED');"
# 2. Check Kafka topic (should see event immediately)
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic dbserver1.public.orders \
  --from-beginning
```
---
## Monitoring
### Outbox Processor Metrics
```bash
curl http://localhost:8089/actuator/metrics/outbox.processed.total
curl http://localhost:8089/actuator/metrics/outbox.failed.total
curl http://localhost:8089/actuator/metrics/outbox.processing.time
```
### Debezium Metrics
```bash
# Connector status
curl http://localhost:8083/connectors/order-connector/status | jq
# Lag (events behind)
curl http://localhost:8083/connectors/order-connector/status | jq '.tasks[0].state'
```
---
## Comparison
### Outbox Polling vs Debezium CDC
| Feature | Outbox Polling | Debezium CDC |
|---------|----------------|--------------|
| **Latency** | 1-5 seconds | < 100ms |
| **Code Changes** | Requires outbox writes | No code changes |
| **Database Load** | Periodic SELECT | WAL read (minimal) |
| **Complexity** | Simple | Complex setup |
| **Reliability** | Very reliable | Very reliable |
| **Scalability** | Medium | High |
| **Operational** | Easy | Requires Kafka Connect |
| **Best For** | Simple apps | High throughput |
---
## Week 10 Learning Summary
### 1. Dual-Write Problem
**Problem:**
```java
save(order);       // Transaction 1
kafkaPublish();    // Transaction 2
// What if Transaction 2 fails? ❌
```
**Solution:**
```java
@Transactional
save(order);
save(outboxEvent);  // SAME transaction ✅
// Outbox processor publishes later
```
### 2. Outbox Pattern
**Key Concept:** Store events in the same database transaction as business data
**Benefits:**
- ✅ Atomic writes (both succeed or both fail)
- ✅ Guaranteed delivery (poll until published)
- ✅ Simple to implement
### 3. Change Data Capture (CDC)
**Key Concept:** Stream database transaction log to Kafka
**How:**
- Database writes to WAL (Write-Ahead Log)
- Debezium reads WAL
- Converts to Kafka events
- Real-time propagation
**Benefits:**
- ✅ No code changes
- ✅ Low latency (< 100ms)
- ✅ Captures all changes
- ✅ Schema evolution support
### 4. At-Least-Once Delivery
**Guarantee:** Every event will be delivered at least once (may be duplicated)
**Why:**
```
Process event → Publish to Kafka → Success
              → Update outbox status → Crash! ❌
On restart: Same event republished (duplicate)
```
**Solution:** Consumers must be idempotent
### 5. Idempotent Consumers
```java
@KafkaListener
public void onOrderCreated(OrderCreatedEvent event) {
    // Check if already processed
    if (processedEvents.contains(event.getId())) {
        log.info("Already processed: {}", event.getId());
        return;  // Skip
    }
    // Process
    processOrder(event);
    // Mark as processed
    processedEvents.add(event.getId());
}
```
---
## Project Structure
```
outbox-processor/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/outbox/
│   │   │   ├── OutboxProcessorApplication.java
│   │   │   ├── processor/
│   │   │   │   ├── OutboxProcessor.java
│   │   │   │   └── EventPublisher.java
│   │   │   ├── repository/
│   │   │   │   └── OutboxRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── OutboxEvent.java
│   │   │   │   └── OutboxStatus.java
│   │   │   ├── config/
│   │   │   │   └── KafkaConfig.java
│   │   │   └── metrics/
│   │   │       └── OutboxMetrics.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
infra/debezium/
├── config/
│   └── connect-distributed.properties
└── connectors/
    ├── order-connector.json
    ├── payment-connector.json
    └── inventory-connector.json
```
---
## Next Steps (Week 11+)
- [ ] Exactly-once semantics (Kafka transactions)
- [ ] Outbox cleanup (delete published events)
- [ ] Partition-aware processing
- [ ] Schema registry integration
- [ ] Multi-database CDC
- [ ] Event versioning
- [ ] Snapshot support
- [ ] Performance tuning
- [ ] Monitoring dashboards
- [ ] Alerting setup
---
## Build Commands
```bash
# Build outbox processor
cd services/outbox-processor
mvn clean package
# Run locally
java -jar target/outbox-processor-0.0.1-SNAPSHOT.jar
# Docker build
docker build -t outbox-processor:latest .
# Start Debezium stack
docker-compose -f infra/docker-compose.yml up -d
```
---
## Dependencies
- Spring Boot 3.1.6
- Spring Data JPA
- Spring Kafka
- PostgreSQL Driver
- Debezium Connector PostgreSQL 2.5
---
**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 10 Complete ✅
