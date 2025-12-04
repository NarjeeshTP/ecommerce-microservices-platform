# Pricing Service

> **Version**: 0.0.1-SNAPSHOT | **Status**: ✅ Production Ready | **Last Updated**: December 3, 2025

Pricing Service manages dynamic pricing rules, discounts, and price calculations for the E-Commerce platform with Redis caching, Prometheus metrics, and resilience patterns.

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
- **Prometheus Metrics** - Request latency, error rate, JVM stats
- **Circuit Breaker** - Resilience4j fault tolerance
- **Database Migrations** - Flyway versioned schema
- **Comprehensive Testing** - 26 tests (unit + integration + e2e)
- **Health Checks** - Database + Redis connectivity

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

