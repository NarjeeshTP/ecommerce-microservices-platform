# Chaos Service & Observability Stack

Complete chaos engineering and observability solution with Chaos Service, OpenTelemetry, Prometheus, Grafana, and Jaeger.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Chaos Service Port:** 8092  
**Status:** Week 12 Implementation Ready

### Features Implemented
- ✅ **Chaos Service** - Fault injection for resilience testing
- ✅ **OpenTelemetry** - Distributed tracing instrumentation
- ✅ **Prometheus** - Metrics collection and alerting
- ✅ **Grafana** - Visualization dashboards
- ✅ **Jaeger** - Distributed trace visualization
- ✅ **Alert Rules** - Automated monitoring alerts
- ✅ **Service Health** - Comprehensive health checks

---

## Components

### 1. Chaos Service
- ✅ Latency injection (configurable delay)
- ✅ Error injection (HTTP 500/503)
- ✅ Kill switch (terminate service)
- ✅ Target-specific scenarios
- ✅ Probability-based activation
- ✅ REST API for chaos control

### 2. OpenTelemetry
- ✅ Automatic trace context propagation
- ✅ Span creation for HTTP requests
- ✅ Trace correlation across services
- ✅ OTLP exporter to Jaeger
- ✅ Sampling configuration

### 3. Prometheus
- ✅ Service metrics scraping
- ✅ Custom business metrics
- ✅ Alert rule evaluation
- ✅ Time-series storage
- ✅ PromQL query language

### 4. Grafana
- ✅ Pre-built dashboards
- ✅ Multiple datasources (Prometheus, Jaeger)
- ✅ Real-time visualization
- ✅ Alert management
- ✅ Dashboard provisioning

### 5. Jaeger
- ✅ Distributed trace collection
- ✅ Trace search and filtering
- ✅ Service dependency graph
- ✅ Latency analysis
- ✅ Error correlation

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- All backend services instrumented with OpenTelemetry

### 1. Start Observability Stack

```bash
# Start Prometheus, Grafana, and Jaeger
docker-compose -f infra/docker-compose-observability.yml up -d

# Verify services
curl http://localhost:9090/-/healthy  # Prometheus
curl http://localhost:3000/api/health  # Grafana
curl http://localhost:16686/          # Jaeger UI
```

### 2. Build & Run Chaos Service

```bash
cd services/chaos-service
mvn clean package -Dmaven.test.skip=true
java -jar target/chaos-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify Setup

```bash
# Check Chaos Service
curl http://localhost:8092/actuator/health | jq

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq

# Access Grafana
open http://localhost:3000
# Login: admin/admin
```

---

## Architecture

### Observability Flow

```
┌──────────────────────────────────────────────────────┐
│  Services (Catalog, Order, Payment, etc.)            │
│                                                      │
│  @RestController                                     │
│  public ResponseEntity<?> getOrders() {              │
│      // Business logic                               │
│      return orders;                                  │
│  }                                                   │
└──────────────────┬───────────────────────────────────┘
                   │
        ┌──────────┼──────────┬────────────┐
        │          │          │            │
        ↓          ↓          ↓            ↓
┌─────────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐
│ Metrics     │ │ Traces  │ │ Logs     │ │ Chaos    │
│ (Actuator)  │ │ (OTEL)  │ │ (Logback)│ │ (Inject) │
└──────┬──────┘ └────┬────┘ └────┬─────┘ └────┬─────┘
       │             │           │            │
       ↓             ↓           ↓            ↓
┌──────────────────────────────────────────────────────┐
│  Observability Stack                                 │
│  ┌──────────────┐  ┌──────────┐  ┌───────────────┐ │
│  │ Prometheus   │  │ Jaeger   │  │ Grafana       │ │
│  │ :9090        │  │ :16686   │  │ :3000         │ │
│  │              │  │          │  │               │ │
│  │ Scrapes      │  │ Collects │  │ Visualizes    │ │
│  │ /actuator/   │  │ traces   │  │ dashboards    │ │
│  │ prometheus   │  │          │  │               │ │
│  └──────────────┘  └──────────┘  └───────────────┘ │
└──────────────────────────────────────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────────────────┐
│  Dashboards & Alerts                                 │
│  - Service Health Dashboard                          │
│  - Business Metrics Dashboard                        │
│  - Trace Analysis                                    │
│  - Alert Notifications (Slack/Email)                 │
└──────────────────────────────────────────────────────┘
```

---

## Chaos Engineering

### What is Chaos Engineering?

**Definition:** Discipline of experimenting on a system to build confidence in its capability to withstand turbulent conditions.

**Purpose:** Find weaknesses before they become outages in production.

### Chaos Scenarios

#### 1. Latency Injection

**Simulate:** Slow network or database

```bash
# Enable latency on order service
curl -X POST http://localhost:8092/api/chaos/latency \
  -H "Content-Type: application/json" \
  -d '{
    "service": "order-service",
    "endpoints": ["/api/orders/**"],
    "delayMs": 2000,
    "probability": 0.5
  }'

# Test affected endpoint
time curl http://localhost:8090/api/orders
# Expect: ~2 second delay on 50% of requests
```

**Expected Behavior:**
- Circuit breaker opens after threshold
- Fallback response returned
- Requests timeout after configured duration

#### 2. Error Injection

**Simulate:** Downstream service failures

```bash
# Enable error injection on payment service
curl -X POST http://localhost:8092/api/chaos/error \
  -H "Content-Type: application/json" \
  -d '{
    "service": "payment-service",
    "endpoints": ["/api/payments/**"],
    "errorType": "HTTP_503",
    "probability": 0.3
  }'

# Test payment flow
curl -X POST http://localhost:8090/api/payments \
  -H "Content-Type: application/json" \
  -d '{"amount": 100, "currency": "USD"}'
# Expect: 30% return 503 Service Unavailable
```

**Expected Behavior:**
- Retry logic activates (3 attempts)
- Exponential backoff delays
- Eventually returns error or succeeds

#### 3. Kill Switch

**Simulate:** Service crash

```bash
# Enable kill switch on inventory service
curl -X POST http://localhost:8092/api/chaos/kill \
  -H "Content-Type: application/json" \
  -d '{
    "service": "inventory-service",
    "probability": 0.01
  }'

# Make requests until service "crashes"
for i in {1..100}; do
  curl http://localhost:8090/api/inventory/check
done
# Expect: Service terminates on ~1% of requests
```

**Expected Behavior:**
- Health check fails
- Kubernetes restarts pod
- Traffic redirects to healthy instances

### Chaos Testing Workflow

```
1. Define Hypothesis
   "System remains available if payment service has 50% error rate"

2. Enable Chaos
   POST /api/chaos/error
   {service: "payment-service", errorRate: 0.5}

3. Generate Load
   Run k6 load test with 100 concurrent users

4. Observe Behavior
   - Check Grafana dashboards
   - Review Jaeger traces
   - Monitor Prometheus alerts

5. Validate Hypothesis
   - Circuit breaker opened? ✓
   - Fallback worked? ✓
   - Users not affected? ✓

6. Disable Chaos
   DELETE /api/chaos/scenarios/payment-service

7. Document Results
   Add findings to runbook
```

---

## OpenTelemetry Integration

### What is OpenTelemetry?

**OpenTelemetry (OTEL):** Vendor-neutral observability framework for tracing, metrics, and logs.

### Trace Anatomy

```
Trace (Order Checkout)
├─ Span 1: API Gateway [200ms]
│  └─ Span 2: Order Service [180ms]
│     ├─ Span 3: Pricing Service [50ms]
│     ├─ Span 4: Inventory Service [30ms]
│     └─ Span 5: Payment Service [80ms]
│        └─ Span 6: Payment Gateway API [70ms]

Total Duration: 200ms
Critical Path: Gateway → Order → Payment → Gateway API
```

### Service Instrumentation

#### Add Dependencies (pom.xml)

```xml
<dependencies>
    <!-- OpenTelemetry -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    
    <!-- Prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

#### Configuration (application.yml)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for dev
  
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
  
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:local}

logging:
  pattern:
    console: "%d [%X{traceId},%X{spanId}] %-5level %logger - %msg%n"
```

### Manual Span Creation

```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public Order createOrder(OrderRequest request) {
        // Create custom span
        Span span = tracer.spanBuilder("order.create")
            .setAttribute("user.id", request.getUserId())
            .setAttribute("order.items", request.getItems().size())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Business logic
            Order order = processOrder(request);
            
            // Add span events
            span.addEvent("order.validated");
            span.addEvent("order.persisted");
            
            // Set final attributes
            span.setAttribute("order.id", order.getId());
            span.setAttribute("order.total", order.getTotalAmount());
            
            return order;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Trace Context Propagation

```
Service A → Service B (HTTP)

Headers:
  traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
  tracestate: congo=t61rcWkgMzE

Decoded:
  version: 00
  trace-id: 4bf92f3577b34da6a3ce929d0e0e4736
  parent-id: 00f067aa0ba902b7
  trace-flags: 01 (sampled)
```

**Automatic Propagation:** Spring Boot automatically adds traceparent header to WebClient/RestTemplate requests

---

## Prometheus Metrics

### Key Metrics

#### HTTP Metrics (Automatic)

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Latency (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Request duration by endpoint
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

#### JVM Metrics (Automatic)

```promql
# Memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# GC pause time
rate(jvm_gc_pause_seconds_sum[5m])

# Thread count
jvm_threads_live_threads
```

#### Custom Business Metrics

```java
@Service
public class OrderService {
    
    private final Counter ordersCreated;
    private final Counter ordersFailed;
    private final Timer orderDuration;
    
    public OrderService(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "checkout")
            .register(registry);
        
        this.ordersFailed = Counter.builder("orders.failed")
            .description("Total orders failed")
            .register(registry);
        
        this.orderDuration = Timer.builder("orders.duration")
            .description("Order processing time")
            .register(registry);
    }
    
    public Order createOrder(OrderRequest request) {
        return orderDuration.record(() -> {
            try {
                Order order = processOrder(request);
                ordersCreated.increment();
                return order;
            } catch (Exception e) {
                ordersFailed.increment();
                throw e;
            }
        });
    }
}
```

### Alert Rules

**High Error Rate:**
```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
  for: 2m
  annotations:
    summary: "Error rate above 5% for 2 minutes"
```

**High Latency:**
```yaml
- alert: HighLatency
  expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
  for: 5m
  annotations:
    summary: "95th percentile latency above 1 second"
```

---

## Grafana Dashboards

### Pre-built Dashboards

#### 1. Service Health Dashboard

**Panels:**
- Service Up/Down status (gauge)
- Request rate per service (graph)
- Error rate per service (graph)
- P50/P95/P99 latency (graph)
- Memory/CPU usage (graph)

**Query Examples:**
```promql
# Service availability
up{job=~".*-service"}

# Request rate
sum(rate(http_server_requests_seconds_count[5m])) by (application)

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

#### 2. Business Metrics Dashboard

**Panels:**
- Orders created (counter)
- Revenue (gauge)
- Payment success rate (graph)
- Cart abandonment rate (graph)
- Top products (table)

**Query Examples:**
```promql
# Orders per minute
rate(orders_created_total[1m]) * 60

# Payment success rate
sum(rate(payments_successful_total[5m])) / sum(rate(payments_total[5m]))

# Average order value
sum(rate(order_total_amount_sum[5m])) / sum(rate(orders_created_total[5m]))
```

#### 3. Distributed Trace Dashboard

**Integration:** Links Grafana → Jaeger

**Features:**
- Click trace ID in Grafana logs
- Opens corresponding trace in Jaeger
- Correlate metrics with traces

### Creating Custom Dashboards

```bash
# Export dashboard JSON
curl http://localhost:3000/api/dashboards/db/service-health \
  -u admin:admin | jq '.dashboard' > dashboard.json

# Import dashboard
curl -X POST http://localhost:3000/api/dashboards/db \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d @dashboard.json
```

---

## Jaeger Distributed Tracing

### Trace Search

```bash
# Find traces by service
http://localhost:16686/search?service=order-service

# Find slow traces (>1s)
http://localhost:16686/search?minDuration=1s

# Find error traces
http://localhost:16686/search?tags=error:true
```

### Trace Analysis

**Example Trace:**
```
Trace: Order Checkout (2.3s total)

1. api-gateway [200ms]
   GET /api/orders
   
2. order-service [1.8s]
   POST /orders
   ├─ db.query [300ms] ← Slow!
   ├─ pricing-service [500ms]
   │  GET /pricing/calculate
   │  └─ cache.miss [450ms] ← Cache miss!
   ├─ inventory-service [100ms]
   │  POST /inventory/reserve
   └─ payment-service [800ms]
      POST /payments/charge
      └─ payment-gateway [750ms] ← External API slow

3. notification-service [300ms]
   POST /notifications/send (async)
```

**Insights:**
- Total: 2.3s (target: <1s)
- Bottleneck: payment-gateway (750ms)
- Optimization: Cache pricing (450ms → 10ms)
- Issue: database query slow (300ms)

### Service Dependency Graph

Jaeger automatically generates service dependency graph from traces:

```
        ┌─────────────┐
        │ api-gateway │
        └──────┬──────┘
               │
        ┌──────▼──────┐
        │order-service│
        └──────┬──────┘
         ┌─────┼─────┬──────────┐
         │     │     │          │
    ┌────▼──┐ ┌▼────┐ ┌────▼────┐ ┌────▼─────┐
    │pricing│ │inven│ │payment  │ │notif     │
    │       │ │tory │ │         │ │          │
    └───────┘ └─────┘ └────┬────┘ └──────────┘
                           │
                      ┌────▼────┐
                      │payment  │
                      │gateway  │
                      └─────────┘
```

---

## Testing Observability

### 1. Generate Traces

```bash
# Create order (generates full trace)
TOKEN=$(curl -X POST http://localhost:8180/realms/myrealm/protocol/openid-connect/token \
  -d "username=user1" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-client" \
  | jq -r '.access_token')

curl -X POST http://localhost:8090/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "items": [{"productId": "LAPTOP-001", "quantity": 1}]
  }'
```

### 2. View in Jaeger

```bash
# Open Jaeger UI
open http://localhost:16686

# Search for traces
Service: order-service
Operation: POST /orders
Lookback: Last 1 hour
```

### 3. Check Metrics

```bash
# Prometheus query
open http://localhost:9090

# Query: Order creation rate
rate(orders_created_total[5m])

# Query: Order processing time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri="/api/orders"}[5m]))
```

### 4. View Dashboards

```bash
# Grafana
open http://localhost:3000

# Login: admin/admin
# Navigate to: Dashboards → Service Health
```

---

## Configuration

### Environment Variables

```bash
# Chaos Service
SERVER_PORT=8092
REDIS_HOST=localhost
REDIS_PORT=6379

# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
OTEL_SERVICE_NAME=order-service
OTEL_TRACES_SAMPLER=always_on

# Prometheus
PROMETHEUS_PORT=9090

# Grafana
GF_SECURITY_ADMIN_PASSWORD=admin
GF_SERVER_HTTP_PORT=3000

# Jaeger
JAEGER_AGENT_PORT=6831
JAEGER_COLLECTOR_PORT=14268
JAEGER_UI_PORT=16686
```

---

## Week 12 Learning Summary

### 1. Chaos Engineering

**Purpose:** Test system resilience by injecting faults

**Scenarios:**
- Latency injection (slow dependencies)
- Error injection (service failures)
- Kill switch (service crashes)

**Benefit:** Find issues before production

### 2. Observability Pillars

#### A. Metrics (Prometheus)
- **What:** Numerical measurements over time
- **Use:** Identify trends, set alerts
- **Examples:** Request rate, error rate, latency

#### B. Traces (Jaeger)
- **What:** Request journey across services
- **Use:** Find bottlenecks, debug errors
- **Examples:** Order checkout trace

#### C. Logs (Optional: Loki)
- **What:** Text records of events
- **Use:** Detailed debugging
- **Examples:** Exception stack traces

### 3. OpenTelemetry

**What:** Standard for instrumenting code

**Benefits:**
- Vendor-neutral
- Automatic instrumentation
- Context propagation

**Result:** Single trace across all services

### 4. Distributed Tracing

**Problem:** How to debug request spanning 10 services?

**Solution:** Distributed tracing

**How:**
```
1. Generate trace ID
2. Propagate via headers
3. Each service creates spans
4. Visualize in Jaeger
```

**Result:** See full request flow in one place

### 5. Metrics vs Traces

| Aspect | Metrics | Traces |
|--------|---------|--------|
| **Cardinality** | Low | High |
| **Storage** | Efficient | Expensive |
| **Use Case** | Trends, alerts | Debugging |
| **Example** | "Error rate: 5%" | "Request ABC failed at step 3" |

---

## Project Structure

```
chaos-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/chaos/
│   │   │   ├── ChaosServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── ChaosController.java
│   │   │   ├── service/
│   │   │   │   ├── LatencyInjector.java
│   │   │   │   ├── ErrorInjector.java
│   │   │   │   └── KillSwitch.java
│   │   │   ├── config/
│   │   │   │   └── ChaosConfig.java
│   │   │   └── model/
│   │   │       └── ChaosScenario.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md

infra/
├── prometheus/
│   ├── prometheus.yml
│   └── alert-rules.yml
├── grafana/
│   ├── dashboards/
│   │   ├── service-health.json
│   │   └── business-metrics.json
│   └── provisioning/
│       ├── datasources/
│       │   └── datasources.yml
│       └── dashboards/
│           └── dashboards.yml
└── jaeger/
    └── jaeger-config.yml
```

---

## Next Steps

- [ ] Add Alertmanager for alert routing
- [ ] Implement Loki for log aggregation
- [ ] Create SLO dashboards
- [ ] Add chaos experiments runbook
- [ ] Implement canary deployments
- [ ] Add performance baselines
- [ ] Create incident response playbook
- [ ] Implement distributed rate limiting

---

**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 12 Complete ✅

