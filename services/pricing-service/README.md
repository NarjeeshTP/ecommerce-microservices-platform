# Pricing Service

> **Version**: 0.0.1-SNAPSHOT | **Status**: ✅ Production Ready | **Last Updated**: December 3, 2025

Pricing Service manages dynamic pricing rules, discounts, and price calculations for the E-Commerce platform with Redis caching, Prometheus metrics, and resilience patterns.

---

## 📑 Table of Contents

- [🚀 Quick Start (3 Steps)](#-quick-start-3-steps)
  - [Port Configuration](#port-configuration)
- [✅ Features](#-features)
- [📋 Prerequisites](#-prerequisites)
- [🔧 Build & Run](#-build--run)
  - [Port Configuration Strategy](#port-configuration-strategy)
  - [Local Development (Recommended)](#local-development-recommended)
  - [Override Port Configuration](#override-port-configuration)
  - [Docker Compose (Full Stack)](#docker-compose-full-stack)
- [🧪 Testing](#-testing)
- [API Endpoints](#api-endpoints)
  - [Pricing Rules Management](#pricing-rules-management)
  - [Price Calculation](#price-calculation)
  - [Cache Management](#cache-management)
- [Caching Behavior](#caching-behavior)
- [Observability & Monitoring](#observability--monitoring)
  - [Health Check](#health-check)
  - [Prometheus Metrics](#prometheus-metrics)
  - [Grafana Dashboard](#grafana-dashboard)
  - [Setup Grafana Alerts](#setup-grafana-alerts)
- [Configuration](#configuration)
  - [Application Properties](#application-properties)
  - [Environment Variables](#environment-variables)
- [Database Schema](#database-schema)
- [📊 Phase 3 Implementation Summary](#-phase-3-implementation-summary)
- [Troubleshooting](#troubleshooting)
- [⚡ Performance Tips](#-performance-tips)
- [🚀 Performance Benchmarking](#-performance-benchmarking)
  - [Prerequisites](#prerequisites-1)
  - [Available Load Test Scripts](#available-load-test-scripts)
  - [Load Test Profiles Comparison](#load-test-profiles-comparison)
  - [When to Use Each Test](#when-to-use-each-test)
  - [Environment Variables](#environment-variables-1)
  - [Test Scenarios](#test-scenarios)
  - [Performance Thresholds](#performance-thresholds)
  - [Expected Results by Load Level](#expected-results-by-load-level)
  - [Analyzing Results](#analyzing-results)
  - [Results Files](#results-files)
  - [Direct k6 Commands](#direct-k6-commands)
  - [Troubleshooting Load Tests](#troubleshooting-load-tests)
  - [Week 4 Deliverables](#week-4-deliverables)
- [🛡️ Circuit Breaker Testing](#️-circuit-breaker-testing)
  - [Circuit Breaker Configuration](#circuit-breaker-configuration)
  - [State Transitions](#state-transitions)
  - [Testing Methods](#testing-methods)
  - [Monitoring Endpoints](#monitoring-endpoints)
  - [Real-Time Monitoring](#real-time-monitoring)
  - [Expected Behavior](#expected-behavior)
  - [Fallback Behavior](#fallback-behavior)
  - [Troubleshooting](#troubleshooting-1)
  - [Integration Tests](#integration-tests)
- [Development Notes](#development-notes)
  - [Key Design Decisions](#key-design-decisions)
  - [Future Enhancements](#future-enhancements)
- [Architecture](#architecture)
- [🎯 Quick Reference Commands](#-quick-reference-commands)
- [Resources](#resources)
- [Support](#support)

---

## 🚀 Quick Start (3 Steps)

```bash
# 1. Start infrastructure
cd infra && docker compose up -d pricing-postgres redis

# 2. Run service (automatically uses port 8083 for local development)
cd ../services/pricing-service && mvn spring-boot:run

# 3. Verify
curl http://localhost:8083/actuator/health
```

### Port Configuration
> **Local Development**: Port **8083** | **Docker**: Port **8082** (prevents conflicts)

**Service URLs**:
- **Local Development** (mvn spring-boot:run): 
  - API: `http://localhost:8083/api/v1/pricing`
  - Health: `http://localhost:8083/actuator/health`
  - Metrics: `http://localhost:8083/actuator/prometheus`
- **Docker Container** (docker compose): 
  - API: `http://localhost:8082/api/v1/pricing`
  - Health: `http://localhost:8082/actuator/health`
  - Metrics: `http://localhost:8082/actuator/prometheus`
- **Monitoring**:
  - Prometheus: `http://localhost:9090`
  - Grafana: `http://localhost:3000` (admin/admin)

**Why Different Ports?**
- Local uses `application-local.yml` profile → port 8083
- Docker uses `default` profile → port 8082
- Both can run simultaneously without conflicts

---

## ✅ Features

- **Dynamic Pricing Rules** - CRUD operations with discounts
- **Redis Caching** - 5-min TTL with auto-invalidation
- **Cache Invalidation Endpoints** - Manual cache control
- **Prometheus Metrics** - Request latency, error rate, JVM stats
- **Circuit Breaker** - Resilience4j with fallback to default values
- **Transaction Timeouts** - 3-second timeout at database transaction level
- **Automatic Fallback** - Returns fallback response on failures/timeouts
- **Database Migrations** - Flyway versioned schema
- **Comprehensive Testing** - 26 tests (unit + integration + e2e)
- **Health Checks** - Database + Redis connectivity
- **Performance Benchmarks** - k6 load testing scripts included

---

## 📋 Prerequisites

- **Java 17+** (OpenJDK 17)
- **Maven 3.6+**
- **Docker & Docker Compose**
- **PostgreSQL 15** (port 5434)
- **Redis 7** (port 6379)

---

## 🔧 Build & Run

### Port Configuration Strategy
The service uses **profile-based port configuration** to prevent conflicts:

| Environment | Profile | Port | How to Run |
|------------|---------|------|------------|
| **Local Development** | `local` | **8083** | `mvn spring-boot:run` |
| **Docker Container** | `default` | **8082** | `docker compose up` |

**Configuration Files**:
- `application.yml` → Default config (port 8082, used by Docker)
- `application-local.yml` → Local config (port 8083, auto-loaded)

**Both Can Run Simultaneously**:
```bash
# Terminal 1: Start Docker
cd infra && docker compose up -d pricing-service

# Terminal 2: Start Local
cd services/pricing-service && mvn spring-boot:run

# Test both
curl http://localhost:8082/actuator/health  # Docker
curl http://localhost:8083/actuator/health  # Local
```

### Local Development (Recommended)
```bash
# Build
mvn clean package -DskipTests

# Start infrastructure
cd ../../infra && docker compose up -d pricing-postgres redis

# Run service - automatically uses port 8083 (local profile)
cd ../services/pricing-service && mvn spring-boot:run

# Verify on port 8083
curl http://localhost:8083/actuator/health
```

### Override Port Configuration

**Option 1: Temporary Port Override**
```bash
# Run on custom port (e.g., 9090)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

**Option 2: Use Docker Port (8082) Locally**
```bash
# Force default profile
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

**Option 3: Change Local Port Permanently**
Edit `src/main/resources/application-local.yml`:
```yaml
server:
  port: 9090  # Your custom port
```

### Docker Compose (Full Stack)
```bash
# All services + monitoring
cd infra
docker compose up -d pricing-postgres redis pricing-service prometheus grafana

# Rebuild specific service
docker compose up --build -d pricing-service

# View logs
docker logs pricing-service -f

# Stop all
docker compose down
```

**Expected Health Response**:
```json
{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

---

## 🧪 Testing

**Test Results**: All 26 tests passing ✅
- 11 controller tests (MockMvc)
- 11 service tests (unit)
- 4 integration tests (Testcontainers)

```bash
# Run all tests
mvn test

# Specific test classes
mvn test -Dtest=PricingControllerTest
mvn test -Dtest=PricingServiceTest
mvn test -Dtest=PricingServiceIntegrationTest

# Build with tests
mvn clean install
```

---

## API Endpoints

> **Note**: Use `localhost:8083` for local development, `localhost:8082` for Docker containers

### Pricing Rules Management

#### Create Pricing Rule
```bash
# Local development (port 8083)
curl -X POST http://localhost:8083/api/v1/pricing/rules \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "LAPTOP-001",
    "basePrice": 1200.00,
    "discountPercent": 20.00,
    "currency": "USD",
    "ruleType": "PROMOTIONAL",
    "status": "ACTIVE"
  }'

# Docker container (port 8082)
curl -X POST http://localhost:8082/api/v1/pricing/rules \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "LAPTOP-001",
    "basePrice": 1200.00,
    "discountPercent": 20.00,
    "currency": "USD",
    "ruleType": "PROMOTIONAL",
    "status": "ACTIVE"
  }'
```

**Response (201 Created)**:
```json
{
  "id": 1,
  "itemId": "LAPTOP-001",
  "basePrice": 1200.00,
  "discountPercent": 20.00,
  "finalPrice": 960.00,
  "currency": "USD",
  "ruleType": "PROMOTIONAL",
  "status": "ACTIVE",
  "createdAt": "2025-11-30T10:30:00Z",
  "updatedAt": "2025-11-30T10:30:00Z"
}
```

#### Get All Pricing Rules
```bash
# Local: curl http://localhost:8083/api/v1/pricing/rules
curl http://localhost:8082/api/v1/pricing/rules  # Docker
```

#### Get Pricing Rule by ID
```bash
curl http://localhost:8083/api/v1/pricing/rules/1  # Local
```

#### Update Pricing Rule
```bash
curl -X PUT http://localhost:8083/api/v1/pricing/rules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "LAPTOP-001",
    "basePrice": 1300.00,
    "discountPercent": 25.00,
    "currency": "USD",
    "ruleType": "SEASONAL",
    "status": "ACTIVE"
  }'
```

#### Delete Pricing Rule
```bash
curl -X DELETE http://localhost:8083/api/v1/pricing/rules/1  # Local
```

### Price Calculation

#### Get Price for Item
```bash
curl http://localhost:8083/api/v1/pricing/price/LAPTOP-001  # Local
```

**Response**:
```json
{
  "itemId": "LAPTOP-001",
  "price": 960.00,
  "currency": "USD",
  "source": "DATABASE",
  "discountApplied": true,
  "originalPrice": 1200.00
}
```

**Note**: First request fetches from database (`source: "DATABASE"`). Subsequent requests use cache (`source: "CACHE"`).

#### Get Price with Quantity (Bulk Pricing)
```bash
curl http://localhost:8083/api/v1/pricing/price/LAPTOP-001/quantity/10  # Local
```

### Cache Management

#### Invalidate Cache for Specific Item
```bash
curl -X POST http://localhost:8083/api/v1/pricing/cache/invalidate/LAPTOP-001  # Local
```

#### Invalidate All Cache
```bash
curl -X POST http://localhost:8083/api/v1/pricing/cache/invalidate-all  # Local
```

---

## Caching Behavior

The service uses **Redis** for caching price calculations:

1. **First Request** → Fetches from database, caches result
2. **Subsequent Requests** → Returns cached value (5 min TTL by default)
3. **Update/Delete Rule** → Automatically invalidates all caches
4. **Manual Invalidation** → Use cache invalidation endpoints

**Cache Keys**:
- Single item: `prices::LAPTOP-001`
- With quantity: `prices::LAPTOP-001_10`

**Verify Cache**:
```bash
# Check Redis keys
docker exec -it infra-redis-1 redis-cli KEYS "*"

# Get cached value
docker exec -it infra-redis-1 redis-cli GET "prices::LAPTOP-001"
```

---

## Observability & Monitoring

### Health Check
```bash
# Local development
curl http://localhost:8083/actuator/health

# Docker container
curl http://localhost:8082/actuator/health
```

### Prometheus Metrics
```bash
# Local development
curl http://localhost:8083/actuator/prometheus

# Docker container
curl http://localhost:8082/actuator/prometheus
```

**Key Metrics**:
- `http_server_requests_seconds` - Request latency
- `resilience4j_circuitbreaker_state` - Circuit breaker status
- `hikaricp_connections_active` - Database connection pool
- `jvm_memory_used_bytes` - JVM memory usage

### Grafana Dashboard

**Access Grafana**: http://localhost:3000  
**Credentials**: admin / admin

**Pre-configured Dashboard**: `Pricing Service Dashboard`

**Panels**:
1. Request Rate (requests/sec)
2. Error Rate (%)
3. Response Time (p50, p95, p99)
4. JVM Memory Usage
5. Database Connection Pool
6. Cache Hit/Miss Ratio
7. Circuit Breaker Status

### Setup Grafana Alerts

1. Open Grafana: http://localhost:3000
2. Navigate to **Alerting → Alert rules**
3. Create new alert rule:

**Example Alert: High Memory Usage**
```yaml
Alert Name: High Memory Usage - Pricing Service
Condition: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
Duration: 5 minutes
Severity: Warning
Message: "Pricing Service memory usage is above 80% for 5 minutes"
```

**Example Alert: High Error Rate**
```yaml
Alert Name: High Error Rate - Pricing Service
Condition: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
Duration: 2 minutes
Severity: Critical
Message: "Pricing Service error rate is above 5%"
```

---

## Configuration

### Application Properties

**Key Configuration** (`application.yml`):

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/pricing_db
    username: pricing_user
    password: pricing_password
  
  data:
    redis:
      host: localhost
      port: 6379
  
  cache:
    redis:
      time-to-live: 300000  # 5 minutes

resilience4j:
  circuitbreaker:
    instances:
      pricingService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000
```

### Environment Variables

Override defaults with environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/pricing_db
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export CACHE_TTL_MILLIS=300000

mvn spring-boot:run
```

---

## Database Schema

**Flyway Migrations**: `src/main/resources/db/migration/`

**Table**: `pricing_rules`

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| item_id | VARCHAR(255) | Product/item identifier |
| base_price | NUMERIC(10,2) | Original price |
| discount_percent | NUMERIC(5,2) | Discount percentage (0-100) |
| final_price | NUMERIC(10,2) | Calculated final price |
| currency | VARCHAR(3) | Currency code (USD, EUR, etc.) |
| rule_type | VARCHAR(50) | Type (PROMOTIONAL, SEASONAL, BULK) |
| min_quantity | INTEGER | Minimum quantity for bulk pricing |
| valid_from | TIMESTAMP | Rule start date |
| valid_until | TIMESTAMP | Rule end date |
| status | VARCHAR(20) | ACTIVE or INACTIVE |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

---

## 📊 Phase 3 Implementation Summary

### Issues Fixed
1. ✅ **Docker Compose YAML** - Fixed corrupted configuration with duplicate keys
2. ✅ **Container Name Conflicts** - Cleaned up orphaned containers
3. ✅ **Compilation Error** - Verified no `enableStatistics()` issue
4. ✅ **Integration Test Failure** - Fixed cache eviction (`@CacheEvict(allEntries = true)`)
5. ✅ **Cache Source Detection** - Now shows "DATABASE" → "CACHE" correctly
6. ✅ **Documentation** - Consolidated into single comprehensive README

### Key Technical Fixes
**Cache Eviction** (`PricingService.java`):
- Changed from `@CacheEvict(value = "prices", key = "#id")` 
- To `@CacheEvict(value = "prices", allEntries = true)`
- Fixed in both `updatePricingRule()` and `deletePricingRule()` methods

**Docker Compose** (`infra/docker-compose.yml`):
- Recreated clean configuration
- Added catalog-postgres, pricing-postgres definitions
- Fixed all health checks and dependencies

### What Was Built
- ✅ Complete pricing rules CRUD API
- ✅ Redis caching with TTL (5 minutes)
- ✅ Automatic cache invalidation on updates
- ✅ Prometheus metrics export
- ✅ Grafana dashboard ready
- ✅ Circuit breaker configuration
- ✅ 26 comprehensive tests (all passing)
- ✅ Flyway database migrations

---

## Troubleshooting

### Common Issues & Solutions

#### Port 8082 Already in Use
**Error**: `Web server failed to start. Port 8082 was already in use.`

**This should only happen if Docker container is already running!**

**Solution 1: Check Docker Container**
```bash
# Check if pricing-service container is running
docker ps | grep pricing-service

# Stop container if running
docker stop pricing-service

# Or use docker compose
cd infra && docker compose stop pricing-service
```

**Solution 2: Use Local Profile (Port 8083)**
```bash
# Local development automatically uses port 8083
mvn spring-boot:run

# Verify it's using port 8083
curl http://localhost:8083/actuator/health
```

**Solution 3: Find What's Using Port 8082**
```bash
# Find process using port 8082
lsof -i:8082

# Kill specific process if needed (not recommended if it's Docker)
lsof -ti:8082 | xargs kill -9
```

**Note**: With the new configuration:
- **Local development (mvn spring-boot:run)** → Port **8083** (no conflict)
- **Docker container** → Port **8082**

#### Database Connection Failed
```bash
# Check postgres is running
docker ps | grep pricing-postgres

# Start if needed
cd infra && docker compose up -d pricing-postgres

# Test connection
docker exec -it pricing-postgres psql -U pricing_user -d pricing_db -c "SELECT 1;"
```

#### Redis Connection Failed
```bash
# Check redis is running
docker ps | grep redis

# Start if needed
cd infra && docker compose up -d redis

# Test redis
docker exec -it infra-redis-1 redis-cli ping  # Expected: PONG
```

#### Container Name Conflicts
```bash
# Remove old containers
docker rm -f pricing-postgres pricing-service

# Or clean all
cd infra
docker compose down --remove-orphans
docker compose up -d
```

#### Cache Not Working (Always "DATABASE")
**Solution**:
1. Verify Redis is running: `docker ps | grep redis`
2. Check logs for Redis connection errors
3. Clear cache: `docker exec -it infra-redis-1 redis-cli FLUSHALL`
4. Verify cache keys: `docker exec -it infra-redis-1 redis-cli KEYS "*"`

#### Docker Compose YAML Errors
```bash
# Validate configuration
cd infra && docker compose config
```

---

## ⚡ Performance Tips

1. **Cache TTL Optimization** - Adjust based on price change frequency:
   ```yaml
   spring.cache.redis.time-to-live: 600000  # 10 minutes for stable prices
   ```

2. **Database Connection Pool** - Tune HikariCP:
   ```yaml
   spring.datasource.hikari:
     maximum-pool-size: 20
     minimum-idle: 5
     connection-timeout: 30000
   ```

3. **JVM Memory** - Set appropriate heap size:
   ```bash
   java -Xms512m -Xmx1024m -jar target/pricing-service-0.0.1-SNAPSHOT.jar
   ```

---

## 🚀 Performance Benchmarking

The Pricing Service includes k6 load testing scripts to validate performance and resilience under load.

### Prerequisites

Install k6:
```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
  sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Windows
choco install k6
```

### Available Load Test Scripts

All load test scripts are in: `scripts/load-tests/`

#### 1. Standard Benchmark (100 users) ✅
```bash
cd scripts/load-tests
./run-benchmark.sh
```
- **Users**: 10 → 50 → 100
- **Duration**: ~4 minutes
- **Purpose**: Standard performance validation
- **Status**: ✅ All thresholds passed

#### 2. Quick Test (Configurable)
```bash
cd scripts/load-tests
./run-quick-test.sh [MAX_USERS]

# Examples:
./run-quick-test.sh         # 200 users (default)
./run-quick-test.sh 300     # 300 users
./run-quick-test.sh 500     # 500 users
```
- **Duration**: ~2 minutes
- **Purpose**: Fast validation after code changes

#### 3. Heavy Load Test (500+ users)
```bash
cd scripts/load-tests
./run-heavy-load-test.sh

# Customize:
MAX_USERS=1000 ./run-heavy-load-test.sh
```
- **Max Users**: 500 (default)
- **Duration**: ~8 minutes
- **Purpose**: Black Friday / Major sale simulation

#### 4. Stress Test (Find Breaking Point)
```bash
cd scripts/load-tests
./run-stress-test.sh

# Customize:
START_USERS=100 MAX_USERS=2000 ./run-stress-test.sh
```
- **Start**: 50 users
- **Max**: 1000 users
- **Increment**: +50 users every minute
- **Purpose**: Find maximum capacity

### Load Test Profiles Comparison

| Script | Users | Duration | Purpose |
|--------|-------|----------|---------|
| **benchmark** | 10→50→100 | 4 min | Standard validation ✅ |
| **quick** | Configurable | 2 min | Fast checks |
| **heavy** | 50→250→500 | 8 min | Peak load simulation |
| **stress** | 50→1000+ | 20+ min | Find limits |

### When to Use Each Test

**Standard Benchmark**:
- ✅ Weekly regression testing
- ✅ Before production deployments
- ✅ After major code changes

**Quick Test**:
- ✅ After minor code changes
- ✅ Quick performance checks
- ✅ CI/CD pipeline validation

**Heavy Load**:
- ✅ Before major sales events (Black Friday)
- ✅ Capacity planning
- ✅ Testing with expected peak load

**Stress Test**:
- ✅ Finding maximum capacity
- ✅ Infrastructure planning
- ✅ Identifying bottlenecks

### Environment Variables

All scripts support these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | http://localhost:8083 | Service URL |
| `MAX_USERS` | Varies | Maximum concurrent users |
| `NORMAL_USERS` | MAX_USERS/2 | Normal load users |
| `RAMP_DURATION` | 30s | Ramp-up time |
| `NORMAL_DURATION` | 1m | Normal load duration |
| `SPIKE_DURATION` | 30s | Spike duration |
| `PEAK_DURATION` | 1m | Peak load duration |

### Test Scenarios

The load tests exercise these endpoints:

1. **Get Price** (cached) - Tests cache hit ratio
2. **Get Price with Quantity** - Tests bulk pricing logic
3. **Get All Rules** (10% of requests) - Tests database read performance
4. **Invalidate Cache** (2% of requests) - Tests cache invalidation

### Performance Thresholds

| Metric | Threshold | Description |
|--------|-----------|-------------|
| Response Time (p95) | < 500ms | 95% of requests under 500ms |
| Error Rate | < 5% | Less than 5% failed requests |
| Custom Error Rate | < 10% | Business logic errors < 10% |

### Expected Results by Load Level

**100 Users (Already Tested)** ✅
```
Response Time (p95): 4.58ms
Error Rate:          0.00%
Status:              ✅ Excellent
```

**200 Users (Tested)** ✅
```
Response Time (p95): 4.25ms
Error Rate:          0.00%
Status:              ✅ Excellent (faster than 100!)
```

**300-500 Users (Estimated)**
```
Response Time (p95): 10-50ms
Error Rate:          < 2%
Status:              ✅ Good
```

**1000+ Users (Stress Test)**
```
Response Time (p95): Varies
Error Rate:          May increase
Status:              Find breaking point
```

### Analyzing Results

**Check Response Times**:
```bash
# View metrics from results
jq '.metrics.http_req_duration' k6-results.json

# Compare different tests
jq '.metrics' k6-quick-results.json | less
```

**Check Error Rates**:
```bash
jq '.metrics.http_req_failed' k6-results.json
```

### Results Files

| File | Script | Description |
|------|--------|-------------|
| `k6-results.json` | benchmark | Standard test (100 users) ✅ |
| `k6-quick-results.json` | quick | Quick test results |
| `k6-heavy-results.json` | heavy | Heavy load results |
| `k6-stress-results.json` | stress | Stress test results |

### Direct k6 Commands

For advanced users:

```bash
cd scripts/load-tests

# Custom user count
k6 run -e MAX_USERS=300 -e NORMAL_USERS=150 k6-load-test.js

# Custom thresholds
k6 run \
  -e MAX_USERS=500 \
  --threshold http_req_duration=p(95)<1000 \
  k6-load-test.js

# Save detailed results
k6 run \
  -e MAX_USERS=1000 \
  --out json=../../my-custom-results.json \
  k6-load-test.js
```

### Analyzing Results

**Console Output**:
The benchmark displays real-time metrics including:
- Request rate (req/s)
- Response times (min, avg, max, p95, p99)
- Error rate
- Cache hit/miss ratio

**JSON Results**:
Detailed results are saved to `k6-results.json`:
```bash
# View summary
cat k6-results.json | jq '.metrics'

# Extract response times
cat k6-results.json | jq '.metrics.http_req_duration'
```

**Visualize with Grafana** (optional):
1. Import k6 results to InfluxDB or Prometheus
2. Create custom dashboard for k6 metrics
3. View trends over multiple test runs

### Troubleshooting Load Tests

**Service Not Running**:
```bash
# Start the service first
mvn spring-boot:run

# Or use Docker
cd ../../infra && docker compose up -d pricing-service
```

**High Error Rate**:
- Check if Redis is running: `docker ps | grep redis`
- Check database connectivity: `docker ps | grep pricing-postgres`
- Review logs: `docker logs pricing-service`

**Slow Response Times**:
- Increase database connection pool: `spring.datasource.hikari.maximum-pool-size`
- Tune Redis: Check network latency to Redis
- Increase cache TTL for more stable prices

**Important Notes Before Running Heavy Tests**:
1. ✅ Ensure service is running
2. ✅ Check database is healthy
3. ✅ Verify Redis is working
4. ✅ Monitor system resources (CPU, Memory)

### Week 4 Deliverables ✅

As part of Phase 3, Week 4 requirements:

- ✅ **Pricing logic, dynamic rules** - CRUD operations with discount calculations
- ✅ **Redis caching with TTL** - 5-minute TTL, configurable
- ✅ **Cache invalidation endpoints** - Manual and automatic invalidation
- ✅ **Resilience: timeouts and fallback** - Circuit breaker + time limiter with fallback responses
- ✅ **Integration tests** - 26 comprehensive tests (unit + integration + e2e)
- ✅ **Performance benchmark** - k6 load testing script with realistic traffic patterns

---

## 🛡️ Circuit Breaker Testing

The Pricing Service uses Resilience4j circuit breaker to protect against cascading failures and provide graceful degradation.

### Circuit Breaker Configuration

From `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pricingService:
        registerHealthIndicator: true
        slidingWindowSize: 10              # Track last 10 calls
        minimumNumberOfCalls: 5            # Need at least 5 calls to calculate failure rate
        permittedNumberOfCallsInHalfOpenState: 3  # Allow 3 test calls in HALF_OPEN
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s        # Wait 5 seconds before transitioning to HALF_OPEN
        failureRateThreshold: 50           # Open circuit if 50% of calls fail
        eventConsumerBufferSize: 10
```

### State Transitions

```
CLOSED ──(50% failures)──> OPEN ──(5 seconds)──> HALF_OPEN ──(3 success)──> CLOSED
   │                          │                       │
   │                          │                       └──(any failure)──> OPEN
   │                          └──(immediate fail fast)
   └──(normal operation)
```

**States Explained**:
- **CLOSED**: Normal operation, requests go through
- **OPEN**: Circuit tripped, requests fail fast with fallback
- **HALF_OPEN**: Testing if service recovered, allows limited test requests

### Testing Methods

#### Method 1: Automated Script (Recommended)

```bash
cd scripts/tests
./test-circuit-breaker.sh
```

**What the script does**:
1. ✅ Checks service health
2. ✅ Creates test pricing rule
3. ✅ Makes successful requests (CLOSED state)
4. ✅ Simulates failures (triggers OPEN state)
5. ✅ Waits for HALF_OPEN transition
6. ✅ Tests recovery to CLOSED state
7. ✅ Shows metrics and events
8. ✅ Cleans up test data

#### Method 2: Manual Testing with curl

**Step 1: Check Initial State**
```bash
curl http://localhost:8083/actuator/health | jq '.components.circuitBreakers'

# Expected: state: "CLOSED"
```

**Step 2: Create Test Data**
```bash
curl -X POST http://localhost:8083/api/v1/pricing/rules \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "TEST-CB-001",
    "basePrice": 100.00,
    "discountPercent": 10,
    "currency": "USD",
    "status": "ACTIVE"
  }'
```

**Step 3: Make Successful Requests (Circuit Stays CLOSED)**
```bash
for i in {1..5}; do
  curl -s http://localhost:8083/api/v1/pricing/price/TEST-CB-001 | jq '.price, .source'
  sleep 0.5
done
```

**Step 4: Trigger Failures (Open the Circuit)**
```bash
# Make requests for non-existent items
for i in {1..8}; do
  echo "Request $i:"
  curl -s http://localhost:8083/api/v1/pricing/price/NON-EXISTENT-$i | jq '.'
  sleep 0.5
done

# Expected: Fallback responses with source: "FALLBACK"
```

**Step 5: Check Circuit is OPEN**
```bash
curl http://localhost:8083/actuator/health | jq '.components.circuitBreakers.details.pricingService.state'

# Expected: "OPEN"
```

**Step 6: Wait for HALF_OPEN (5 seconds)**
```bash
sleep 5
curl http://localhost:8083/actuator/health | jq '.components.circuitBreakers.details.pricingService.state'

# Expected: "HALF_OPEN"
```

**Step 7: Test Recovery**
```bash
# Make successful requests in HALF_OPEN state
for i in {1..3}; do
  curl -s http://localhost:8083/api/v1/pricing/price/TEST-CB-001 | jq '.price, .source'
  sleep 1
done

# After successful requests, circuit should transition to CLOSED
sleep 2
curl http://localhost:8083/actuator/health | jq '.components.circuitBreakers.details.pricingService.state'

# Expected: "CLOSED"
```

### Monitoring Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Circuit breaker state and health |
| `/actuator/metrics/resilience4j.circuitbreaker.state` | Current state (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |
| `/actuator/metrics/resilience4j.circuitbreaker.failure.rate` | Failure rate percentage |
| `/actuator/circuitbreakers` | All circuit breakers info |
| `/actuator/circuitbreakerevents` | Recent circuit breaker events |

### Real-Time Monitoring

```bash
# Watch circuit breaker state
watch -n 1 'curl -s http://localhost:8083/actuator/health | jq .components.circuitBreakers.details.pricingService.state'

# Monitor failure rate
watch -n 1 'curl -s http://localhost:8083/actuator/metrics/resilience4j.circuitbreaker.failure.rate | jq .measurements[0].value'

# View recent events
curl -s http://localhost:8083/actuator/circuitbreakerevents | jq '.circuitBreakerEvents | map({timestamp, type, state: .stateTransition})'
```

### Expected Behavior

**Scenario 1: Normal Operation**
- State: CLOSED
- Behavior: All requests go through to repository
- Response: Normal price responses
- Failure Rate: 0%

**Scenario 2: Increasing Failures**
- Initial State: CLOSED
- Action: 5+ requests with 50%+ failures
- Transition: CLOSED → OPEN
- Behavior: Circuit opens, requests fail fast
- Response: Fallback responses (price=0, source="FALLBACK")

**Scenario 3: Circuit Open**
- State: OPEN
- Behavior: Requests fail immediately without calling repository
- Response: Immediate fallback
- Duration: Stays open for 5 seconds

**Scenario 4: Recovery Attempt**
- Initial State: OPEN
- Action: Wait 5 seconds
- Transition: OPEN → HALF_OPEN
- Behavior: Allows 3 test requests
- If Success: HALF_OPEN → CLOSED (circuit recovers)
- If Failure: HALF_OPEN → OPEN (circuit remains broken)

### Fallback Behavior

When the circuit breaker is triggered, the fallback method returns:

```json
{
  "itemId": "requested-item-id",
  "price": 0,
  "currency": "USD",
  "source": "FALLBACK",
  "discountApplied": false,
  "originalPrice": null
}
```

**Key Point**: Returns HTTP 200 with fallback data instead of HTTP 500, providing graceful degradation.

### Troubleshooting

**Circuit Not Opening**:
- Need `minimumNumberOfCalls` (5+)
- Need 50%+ failure rate
- Check: `curl http://localhost:8083/actuator/metrics/resilience4j.circuitbreaker.failure.rate`

**Circuit Not Transitioning to HALF_OPEN**:
- Wait full `waitDurationInOpenState` (5 seconds)
- Make a request after waiting (triggers transition)
- Check: `curl http://localhost:8083/actuator/health`

**Fallback Not Working**:
- Verify `@CircuitBreaker` annotation on service methods
- Check fallback method signature matches
- Review application logs

### Integration Tests

Run the circuit breaker integration tests:

```bash
mvn test -Dtest=CircuitBreakerIntegrationTest

# Expected: 5 tests, all passing ✅
```

**Test Coverage**:
1. ✅ Circuit opens after failure threshold
2. ✅ Fallback method invoked when circuit open
3. ✅ Circuit transitions to HALF_OPEN after wait duration
4. ✅ Circuit closes after successful recovery
5. ✅ Metrics are accurately tracked

---

## Development Notes

### Key Design Decisions

1. **Calculated finalPrice** - Stored in database to avoid recalculation on every request
2. **Cache Invalidation** - Evicts all caches on update/delete for consistency
3. **Resilience4j** - Circuit breaker prevents cascade failures
4. **Testcontainers** - Integration tests use real PostgreSQL

### Future Enhancements

- [ ] Add time-based pricing rules (happy hour, seasonal)
- [ ] Support multiple currencies with exchange rates
- [ ] Implement price change history/audit log
- [ ] Add A/B testing for pricing strategies
- [ ] Support customer-specific pricing tiers

---

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│  Pricing Service (8082)     │
│  - PricingController        │
│  - PricingService           │
│  - Cache Interceptor        │
└──────┬──────────────┬───────┘
       │              │
       ▼              ▼
┌──────────┐   ┌──────────┐
│PostgreSQL│   │  Redis   │
│  :5434   │   │  :6379   │
└──────────┘   └──────────┘
```

---

## 🎯 Quick Reference Commands

### Start Service
```bash
cd infra && docker compose up -d pricing-postgres redis
cd ../services/pricing-service && mvn spring-boot:run
```

### Verify Health
```bash
# Local (port 8083)
curl http://localhost:8083/actuator/health

# Docker (port 8082)
curl http://localhost:8082/actuator/health
```

### Create Sample Pricing Rule
```bash
curl -X POST http://localhost:8083/api/v1/pricing/rules \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "LAPTOP-001",
    "basePrice": 1200.00,
    "discountPercent": 20.00,
    "currency": "USD",
    "ruleType": "PROMOTIONAL",
    "status": "ACTIVE"
  }'
```

### Test Cache Behavior
```bash
# First call - DATABASE
curl http://localhost:8083/api/v1/pricing/price/LAPTOP-001

# Second call - CACHE
curl http://localhost:8083/api/v1/pricing/price/LAPTOP-001
```

### Check Metrics
```bash
curl http://localhost:8083/actuator/prometheus | grep http_server_requests
```

### View Logs
```bash
docker logs pricing-service -f
docker logs pricing-postgres -f
docker logs infra-redis-1 -f
```

### Run Tests
```bash
cd services/pricing-service && mvn test
```

### Common Fixes
```bash
# Kill port 8082
lsof -ti:8082 | xargs kill -9

# Clean containers
cd infra && docker compose down --remove-orphans

# Check containers
docker ps | grep -E "(pricing|redis)"

# Verify cache keys
docker exec -it infra-redis-1 redis-cli KEYS "*"
```

---

## Resources

- **API Specification**: `docs/api/pricing.yaml`
- **Architecture Docs**: `docs/architecture/`
- **Event Schemas**: `docs/events/avro-schemas/`

---

## Support

For issues or questions:
1. Check logs: `docker logs pricing-service -f`
2. Verify infrastructure: `docker compose ps`
3. Test endpoints: Use the API examples above
4. Check metrics: `curl http://localhost:8082/actuator/prometheus`

---

**Last Updated**: December 3, 2025  
**Service Version**: 0.0.1-SNAPSHOT

