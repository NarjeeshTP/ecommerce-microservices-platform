# Correlation ID Filter - Complete Reference Guide

**Status:** ✅ IMPLEMENTED  
**Date:** November 29, 2025  
**Service:** Catalog Service  
**Version:** 1.0

---

## 📋 Table of Contents

1. [What is Correlation ID](#what-is-correlation-id)
2. [Why It's Essential](#why-its-essential)
3. [Implementation Details](#implementation-details)
4. [How to Use](#how-to-use)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)
7. [Future Enhancements](#future-enhancements)

---

## What is Correlation ID

A **Correlation ID** is a unique identifier (UUID) that tracks a single user request across multiple services, operations, and log entries in a distributed system.

### Key Concept
```
ONE Request = ONE Correlation ID = Track ENTIRE Journey
```

**Example:**
```
User Request [correlation-id: abc-123]
├── Catalog Service [abc-123] - Browse items
├── Pricing Service [abc-123] - Get prices
├── Inventory Service [abc-123] - Check stock
└── Order Service [abc-123] - Create order
```

Search logs for `abc-123` → See complete request flow across all services! 🎯

---

## Why It's Essential

### The Problem Without Correlation ID ❌

**Logs from multiple services:**
```
[Catalog] 14:30:15 INFO - Item fetched: LAP-001
[Catalog] 14:30:15 ERROR - Database timeout
[Pricing] 14:30:16 INFO - Price calculated for LAP-001
[Inventory] 14:30:16 ERROR - Item not found
```

**Question:** Which error belongs to which user request? **Answer:** Impossible to know! 😵

### The Solution With Correlation ID ✅

```
[abc-123] [Catalog] 14:30:15 INFO - Item fetched: LAP-001
[abc-123] [Catalog] 14:30:15 ERROR - Database timeout     ← User A's error
[xyz-789] [Pricing] 14:30:16 INFO - Price calculated      ← User B's request
[xyz-789] [Inventory] 14:30:16 ERROR - Item not found    ← User B's error
```

**Question:** Which error belongs to which user? **Answer:** Crystal clear! 😎

### Key Benefits

| Benefit | Description | Example |
|---------|-------------|---------|
| 🔍 **Distributed Tracing** | Track requests across services | `grep "abc-123" services/*/logs/*.log` |
| 🐛 **Easy Debugging** | Find all logs for one request | Search one ID = see everything |
| 💬 **Better Support** | Customer provides correlation ID | Instant root cause analysis |
| ⏱️ **Performance Monitoring** | Measure end-to-end latency | Track slow operations |
| 🔒 **Audit Trails** | Complete user journey tracking | Compliance (SOC2, GDPR) |
| 📊 **Observability** | Integration with ELK, Grafana | Dashboard filters by ID |

---

## Implementation Details

### Files Implemented

#### 1. Core Filter
**File:** `src/main/java/com/ecommerce/catalogservice/config/CorrelationIdFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Set in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response headers
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue chain
            chain.doFilter(request, response);
        } finally {
            // Critical: Clean up MDC
            MDC.clear();
        }
    }
}
```

**Key Points:**
- ✅ Runs before all other filters (`HIGHEST_PRECEDENCE`)
- ✅ Auto-generates UUID if no header provided
- ✅ Stores in MDC (thread-local) for logging
- ✅ Returns in response header
- ✅ Cleans up MDC in `finally` block (prevents memory leaks)

#### 2. Logging Configuration
**File:** `src/main/resources/logback-spring.xml`

```xml
<configuration>
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{correlationId:-NO-CORRELATION-ID}] [%thread] %-5level %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/catalog-service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/catalog-service.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Key Points:**
- ✅ `%X{correlationId}` includes MDC value in logs
- ✅ Falls back to `NO-CORRELATION-ID` if not set
- ✅ Rolling file policy (30 days retention)

#### 3. Error Response Integration
**File:** `src/main/java/com/ecommerce/catalogservice/exception/GlobalExceptionHandler.java`

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(...) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .correlationId(MDC.get("correlationId"))  // ← Includes correlation ID
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
```

---

## How to Use

### 1. Making Requests with Correlation ID

#### Option A: Auto-Generated (Default)
```bash
curl http://localhost:8081/catalog/items
```

**Response Header:**
```
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
```

#### Option B: Custom Correlation ID
```bash
curl -H "X-Correlation-ID: my-custom-id-123" \
     http://localhost:8081/catalog/items
```

**Response Header:**
```
X-Correlation-ID: my-custom-id-123
```

### 2. Tracking Across Multiple Operations

```bash
# Set your correlation ID
export SESSION_ID="user-session-$(date +%s)"

# Operation 1
curl -H "X-Correlation-ID: $SESSION_ID" \
     http://localhost:8081/catalog/items

# Operation 2
curl -H "X-Correlation-ID: $SESSION_ID" \
     http://localhost:8081/catalog/items/1

# Operation 3
curl -H "X-Correlation-ID: $SESSION_ID" \
     "http://localhost:8081/catalog/items/search?category=Electronics"

# View all operations
grep "$SESSION_ID" logs/catalog-service.log
```

**Output:**
```
[user-session-1732847123] INFO - Fetching all items
[user-session-1732847123] INFO - Fetching item with id: 1
[user-session-1732847123] INFO - Searching items
```

**🎉 Same ID across all operations = Complete tracking!**

### 3. Error Response Example

**Request:**
```bash
curl -H "X-Correlation-ID: error-test-123" \
     http://localhost:8081/catalog/items/99999
```

**Response:**
```json
{
  "timestamp": "2025-11-29T14:30:25.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Item not found with id: '99999'",
  "path": "/catalog/items/99999",
  "correlationId": "error-test-123"  ← Customer can provide this to support!
}
```

### 4. Searching Logs

```bash
# Find all operations for a specific correlation ID
grep "abc-123" logs/catalog-service.log

# With context (5 lines before/after)
grep -A 5 -B 5 "abc-123" logs/catalog-service.log

# Count operations for a correlation ID
grep "abc-123" logs/catalog-service.log | wc -l

# Search across all services (when implemented)
grep "abc-123" services/*/logs/*.log
```

---

## Testing

### Unit Tests (8 tests)
```bash
mvn test -Dtest=CorrelationIdFilterTest
```

**Tests cover:**
- ✅ Auto-generation of UUID
- ✅ Custom correlation ID preservation
- ✅ MDC lifecycle management
- ✅ Edge cases (blank, empty IDs)

### Integration Tests (3 tests)
```bash
mvn test -Dtest=CatalogControllerIntegrationTest
```

**Tests cover:**
- ✅ Correlation ID in response headers
- ✅ Custom correlation ID preservation
- ✅ Correlation ID in error responses

### Manual Testing Scripts

```bash
# Interactive step-by-step test
./test-correlation-manual.sh

# Quick automated test
./quick-test-correlation.sh

# Comprehensive test with API calls
./test-correlation-id.sh
```

### Verification Checklist

- [ ] All response headers include `X-Correlation-ID`
- [ ] Custom correlation IDs are preserved
- [ ] Logs include `[correlation-id]` format
- [ ] Error responses include `correlationId` field
- [ ] Can search logs by correlation ID
- [ ] Each request has unique ID (if auto-generated)

---

## Troubleshooting

### Issue: No correlation ID in response headers

**Check:**
```bash
curl -v http://localhost:8081/catalog/items 2>&1 | grep "X-Correlation-ID"
```

**Solution:** 
- Verify `CorrelationIdFilter` has `@Component` annotation
- Check filter is registered in Spring context
- Restart service

### Issue: Logs don't show correlation ID

**Check:**
```bash
tail -20 logs/catalog-service.log
```

**Expected format:**
```
[correlation-id] INFO - Message
```

**Solution:**
- Verify `logback-spring.xml` exists in `src/main/resources/`
- Check pattern includes `%X{correlationId}`
- Restart service to reload configuration

### Issue: correlationId is null in error responses

**Check error response:**
```bash
curl http://localhost:8081/catalog/items/99999 | python3 -m json.tool
```

**Possible causes:**
1. Filter not executing → Check filter order
2. MDC not set → Check filter code
3. MDC cleared too early → Check finally block

**Solution:** Verify `GlobalExceptionHandler` uses `MDC.get("correlationId")`

### Issue: Different correlation IDs in logs for same request

**This should NOT happen!** If it does:
- Check if MDC is being cleared prematurely
- Verify thread-local context is preserved
- Check for async operations (they need special handling)

---

## Future Enhancements

### 1. Propagation to Downstream Services

When Catalog Service calls other microservices, propagate the correlation ID:

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add interceptor to propagate correlation ID
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                request.getHeaders().set("X-Correlation-ID", correlationId);
            }
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
}
```

**Apply to:**
- ✅ Pricing Service
- ✅ Inventory Service
- ✅ Order Service
- ✅ Payment Service
- ✅ Cart Service

### 2. Kafka Event Correlation

Add correlation ID to Kafka message headers:

```java
public void sendOrderCreatedEvent(Order order) {
    ProducerRecord<String, OrderCreatedEvent> record = 
        new ProducerRecord<>("order-created", event);
    
    // Add correlation ID to Kafka headers
    String correlationId = MDC.get("correlationId");
    if (correlationId != null) {
        record.headers().add("X-Correlation-ID", 
                            correlationId.getBytes(StandardCharsets.UTF_8));
    }
    
    kafkaTemplate.send(record);
}
```

**Consumer side:**
```java
@KafkaListener(topics = "order-created")
public void handleOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
    // Extract correlation ID from headers
    Header correlationIdHeader = record.headers().lastHeader("X-Correlation-ID");
    if (correlationIdHeader != null) {
        String correlationId = new String(correlationIdHeader.value(), StandardCharsets.UTF_8);
        MDC.put("correlationId", correlationId);
    }
    
    try {
        // Process event with correlation ID in logs
        processOrder(record.value());
    } finally {
        MDC.clear();
    }
}
```

### 3. OpenAPI/Swagger Documentation

Document the correlation ID header:

```yaml
openapi: 3.0.0
paths:
  /catalog/items:
    get:
      parameters:
        - name: X-Correlation-ID
          in: header
          description: Request correlation ID for distributed tracing
          required: false
          schema:
            type: string
            format: uuid
      responses:
        '200':
          headers:
            X-Correlation-ID:
              description: Correlation ID for tracking this request
              schema:
                type: string
                format: uuid
```

### 4. Monitoring Integration

#### ELK Stack
```json
{
  "message": "Fetching all items",
  "correlationId": "abc-123",
  "service": "catalog-service",
  "timestamp": "2025-11-29T14:30:15.123Z"
}
```

**Kibana Query:**
```
correlationId: "abc-123"
```

#### Grafana Dashboard
- Create variable: `$correlationId`
- Filter all panels by correlation ID
- Track metrics per request

#### Prometheus Metrics
```java
@Timed(value = "api.request", extraTags = {"correlationId", "#{MDC.get('correlationId')}"})
public List<ItemDTO> getAllItems() {
    // Implementation
}
```

### 5. Distributed Tracing Tools

#### Spring Cloud Sleuth (Alternative)
For automatic correlation ID + more features:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

**Pros:** Automatic propagation, Zipkin integration  
**Cons:** Heavier dependency, less control

#### Zipkin/Jaeger Integration
Use correlation ID as trace ID:

```java
brave.Span span = tracer.currentSpan();
if (span != null) {
    span.tag("correlationId", MDC.get("correlationId"));
}
```

---

## Quick Reference

### Key Commands

```bash
# Generate correlation ID
export CID="test-$(date +%s)"

# Make request
curl -H "X-Correlation-ID: $CID" http://localhost:8081/catalog/items

# Search logs
grep "$CID" logs/catalog-service.log

# Count operations
grep "$CID" logs/catalog-service.log | wc -l

# Run tests
mvn test -Dtest=CorrelationIdFilterTest
./test-correlation-manual.sh
```

### Important Files

| File | Purpose |
|------|---------|
| `CorrelationIdFilter.java` | Core filter implementation |
| `logback-spring.xml` | Logging configuration |
| `GlobalExceptionHandler.java` | Error response integration |
| `CorrelationIdFilterTest.java` | Unit tests |
| `test-correlation-manual.sh` | Manual testing script |

### Log Format

```
2025-11-29 14:30:15.123 [correlation-id] [thread-name] LEVEL Logger - Message
                         ^^^^^^^^^^^^^^
                         Your correlation ID
```

### HTTP Headers

**Request:**
```
X-Correlation-ID: my-custom-id
```

**Response:**
```
X-Correlation-ID: my-custom-id
```

---

## Best Practices

### 1. Naming Convention
```bash
# Good - descriptive
customer-checkout-1732847123
user-john-session-1732847123
api-gateway-request-1732847123

# Acceptable - simple
test-1732847123
session-1732847123

# Avoid - meaningless
abc123
test
```

### 2. When to Use Custom IDs
- ✅ API Gateway (generate once, propagate to all services)
- ✅ Testing (track specific test scenarios)
- ✅ Customer support (user provides ID)
- ❌ Regular application flow (auto-generate)

### 3. Log Search Best Practices
```bash
# Include context
grep -A 5 -B 5 "correlation-id" logs/catalog-service.log

# Search with timestamp
grep "correlation-id" logs/catalog-service.log | awk '{print $1, $2, $NF}'

# Export for analysis
grep "correlation-id" logs/catalog-service.log > session-analysis.txt
```

### 4. Production Monitoring
- Set up alerts for requests without correlation ID
- Track correlation ID coverage (% of requests with IDs)
- Monitor MDC cleanup (memory leak detection)
- Index correlation ID in log aggregation tools

---

## Summary

### What Was Implemented ✅
- Core filter (`CorrelationIdFilter.java`)
- Logging configuration (`logback-spring.xml`)
- Error response integration
- 11 comprehensive tests
- Testing scripts and documentation

### Benefits Delivered 🎁
- 🔍 Distributed tracing across operations
- 🐛 Easy debugging (search one ID = see everything)
- 💬 Better customer support
- ⏱️ Performance monitoring
- 🔒 Complete audit trails

### Next Steps 🚀
1. Copy to other services (Pricing, Inventory, Order, Payment, Cart)
2. Add RestTemplate/WebClient propagation
3. Add Kafka event correlation
4. Document in OpenAPI/Swagger
5. Integrate with ELK/Grafana

### Test It Now 🧪
```bash
cd services/catalog-service
./test-correlation-manual.sh
```

---

**Implementation Status:** ✅ Production Ready  
**Test Coverage:** Excellent (11 tests)  
**Documentation:** Complete  
**Ready for Deployment:** YES

**Alhamdulillah!** 🤲

---

*Last Updated: November 29, 2025*  
*Version: 1.0*  
*Service: Catalog Service*

