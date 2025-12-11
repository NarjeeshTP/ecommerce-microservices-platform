# API Gateway

Spring Cloud Gateway with JWT validation, rate limiting, and routing to all microservices.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8090  
**Status:** Week 11 Implementation Ready

### Features Implemented
- ✅ **Spring Cloud Gateway** - Reactive API Gateway
- ✅ **JWT Validation** - Keycloak OAuth2 integration
- ✅ **Rate Limiting** - Redis-based rate limiting per route
- ✅ **Service Routing** - Routes to 8 backend services
- ✅ **CORS Configuration** - Cross-origin resource sharing
- ✅ **Retry Logic** - Automatic retry with exponential backoff
- ✅ **Public/Private Paths** - Authentication bypass for public endpoints
- ✅ **Metrics & Monitoring** - Prometheus integration

---

## Features

### Core Functionality
- ✅ **Centralized Entry Point** - Single endpoint for all services
- ✅ **Authentication** - JWT token validation via Keycloak
- ✅ **Authorization** - Role-based access control
- ✅ **Rate Limiting** - Prevent API abuse
- ✅ **Load Balancing** - Distribute requests across instances
- ✅ **Circuit Breaking** - Fail fast on downstream errors
- ✅ **Request/Response Logging** - Audit trail

### Technical Features
- ✅ **Reactive Stack** - Built on Project Reactor (WebFlux)
- ✅ **Redis Rate Limiter** - Token bucket algorithm
- ✅ **OAuth2 Resource Server** - Validates JWTs from Keycloak
- ✅ **Global Filters** - Retry, CORS, logging
- ✅ **Route Predicates** - Path-based routing
- ✅ **Dynamic Configuration** - Environment-based routing

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- Redis (port 6379)
- Keycloak (port 8180)
- All backend services running

### 1. Start Dependencies

```bash
# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Keycloak
docker run -d --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:23.0.0 start-dev
```

### 2. Build & Run

```bash
cd services/api-gateway
mvn clean package -Dmaven.test.skip=true
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
# Health check
curl http://localhost:8090/actuator/health | jq

# Gateway routes
curl http://localhost:8090/actuator/gateway/routes | jq
```

---

## Architecture

### Gateway Flow

```
┌─────────────────────────────────────────────────────────┐
│  Client (Browser/Mobile)                                │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTP Request
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  API Gateway (Port 8090)                                │
│                                                         │
│  ┌────────────────────────────────────────────┐       │
│  │  1. CORS Filter                            │       │
│  │     - Allow origins                        │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  2. Rate Limiter (Redis)                   │       │
│  │     - Check tokens available               │       │
│  │     - Return 429 if exceeded               │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  3. JWT Validation (Keycloak)              │       │
│  │     - Extract token from Authorization     │       │
│  │     - Validate signature                   │       │
│  │     - Check expiration                     │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  4. Route Matching                         │       │
│  │     - /api/catalog/** → Catalog Service    │       │
│  │     - /api/orders/** → Order Service       │       │
│  │     - /api/payments/** → Payment Service   │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  5. Load Balancing (if multiple instances) │       │
│  └────────────────────────────────────────────┘       │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬──────────────┐
        │             │             │              │
        ↓             ↓             ↓              ↓
┌──────────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────┐
│Catalog:8080  │ │Order:8084│ │Payment:8085 │ │Cart:8083 │
└──────────────┘ └──────────┘ └─────────────┘ └──────────┘
```

---

## Configuration

### Routes Configuration

#### Catalog Service (Public)
```yaml
- id: catalog-service
  uri: http://localhost:8080
  predicates:
    - Path=/api/catalog/**, /api/products/**
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 100
        redis-rate-limiter.burstCapacity: 200
```

**Rate Limit:** 100 requests/second, burst up to 200

#### Order Service (Protected)
```yaml
- id: order-service
  uri: http://localhost:8084
  predicates:
    - Path=/api/orders/**
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 30
        redis-rate-limiter.burstCapacity: 60
```

**Rate Limit:** 30 requests/second, burst up to 60

### JWT Validation

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/myrealm
          jwk-set-uri: http://localhost:8180/realms/myrealm/protocol/openid-connect/certs
```

**Public Paths (No JWT Required):**
- `/api/catalog/**`
- `/api/products/**`
- `/api/search/**`
- `/actuator/**`

**Protected Paths (JWT Required):**
- `/api/cart/**`
- `/api/orders/**`
- `/api/payments/**`
- `/api/inventory/**`

---

## Rate Limiting

### How It Works (Token Bucket Algorithm)

```
Token Bucket:
┌──────────────────────────────┐
│  Capacity: 200 tokens        │
│  Refill Rate: 100/second     │
│                              │
│  [🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙]        │
│  150 tokens available        │
└──────────────────────────────┘

Request arrives:
  - Take 1 token from bucket ✅
  - If bucket empty → 429 Too Many Requests ❌
  - Tokens refill at 100/second
```

### Configuration Per Service

| Service | Replenish Rate | Burst Capacity |
|---------|----------------|----------------|
| **Catalog** | 100/sec | 200 |
| **Search** | 100/sec | 200 |
| **Cart** | 50/sec | 100 |
| **Inventory** | 50/sec | 100 |
| **Pricing** | 50/sec | 100 |
| **Order** | 30/sec | 60 |
| **Payment** | 20/sec | 40 |

### Testing Rate Limiting

```bash
# Send 100 requests rapidly
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/api/catalog/products &
done
wait

# Expected: First ~50 return 200, rest return 429
```

---

## JWT Authentication

### Getting a Token from Keycloak

```bash
# Login and get access token
TOKEN=$(curl -X POST http://localhost:8180/realms/myrealm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user1" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-client" \
  | jq -r '.access_token')

echo $TOKEN
```

### Making Authenticated Requests

```bash
# Public endpoint (no token needed)
curl http://localhost:8090/api/catalog/products | jq

# Protected endpoint (token required)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/api/orders | jq

# Without token → 401 Unauthorized
curl http://localhost:8090/api/orders
```

---

## Testing

### Test Routing

```bash
# Catalog Service via Gateway
curl http://localhost:8090/api/catalog/products | jq

# Order Service via Gateway
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/api/orders | jq

# Cart Service via Gateway
curl -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: user-123" \
  http://localhost:8090/api/cart | jq
```

### Test CORS

```bash
# Preflight request
curl -X OPTIONS http://localhost:8090/api/catalog/products \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

### Test Retry Logic

```bash
# Gateway retries up to 3 times on 502/504
# Simulate by stopping backend service
docker stop catalog-service

# Request fails after 3 retries
curl http://localhost:8090/api/catalog/products
```

---

## Monitoring

### Gateway Routes

```bash
curl http://localhost:8090/actuator/gateway/routes | jq
```

### Gateway Metrics

```bash
# Route metrics
curl http://localhost:8090/actuator/metrics/spring.cloud.gateway.requests | jq

# Rate limiter metrics
curl http://localhost:8090/actuator/metrics/gateway.rate.limiter.requests | jq
```

### Health Check

```bash
curl http://localhost:8090/actuator/health | jq
```

---

## Troubleshooting

### 401 Unauthorized

```
Error: JWT validation failed
```

**Causes:**
- Token expired
- Invalid signature
- Token from wrong issuer
- Keycloak not reachable

**Solutions:**
```bash
# Get fresh token
TOKEN=$(curl -X POST http://localhost:8180/realms/myrealm/protocol/openid-connect/token ...)

# Check Keycloak
curl http://localhost:8180/realms/myrealm/.well-known/openid-configuration
```

### 429 Too Many Requests

```
Error: Rate limit exceeded
```

**Solution:** Wait or increase rate limits in application.yml

### 503 Service Unavailable

```
Error: No instances available for service
```

**Cause:** Backend service is down

**Solution:**
```bash
# Check backend service
curl http://localhost:8080/actuator/health

# Restart backend
docker start catalog-service
```

---

## Week 11 Learning Summary

### 1. API Gateway Pattern

**Purpose:** Single entry point for all microservices

**Benefits:**
- ✅ Centralized authentication
- ✅ Rate limiting
- ✅ CORS handling
- ✅ Request routing
- ✅ Load balancing

### 2. JWT Validation

**Flow:**
```
1. Client → Login to Keycloak → Get JWT
2. Client → API Gateway (JWT in Authorization header)
3. Gateway → Validate JWT signature with Keycloak public key
4. Gateway → Extract user info from JWT claims
5. Gateway → Route to backend service
```

### 3. Token Bucket Rate Limiting

**Algorithm:**
```java
// Simplified concept
class TokenBucket {
    int capacity = 200;
    int tokens = 200;
    int refillRate = 100; // per second
    
    boolean allowRequest() {
        if (tokens > 0) {
            tokens--;
            return true;  // Allow
        }
        return false;  // Rate limited
    }
    
    void refill() {
        tokens = min(capacity, tokens + refillRate);
    }
}
```

### 4. Reactive Gateway

**Why Reactive?**
- Non-blocking I/O
- High throughput
- Low memory footprint

**Stack:**
- Spring WebFlux (reactive web)
- Project Reactor (reactive streams)
- Netty (async server)

---

## Configuration

### Environment Variables

```bash
# Backend Services
CATALOG_SERVICE_URL=http://localhost:8080
ORDER_SERVICE_URL=http://localhost:8084
PAYMENT_SERVICE_URL=http://localhost:8085
CART_SERVICE_URL=http://localhost:8083

# Keycloak
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/myrealm
KEYCLOAK_JWK_URI=http://localhost:8180/realms/myrealm/protocol/openid-connect/certs

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Server
SERVER_PORT=8090
```

---

## Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/gateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   ├── filter/
│   │   │   │   ├── AuthenticationFilter.java
│   │   │   │   └── LoggingFilter.java
│   │   │   └── exception/
│   │   │       └── GatewayExceptionHandler.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Build Commands

```bash
# Build
mvn clean package

# Run
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar

# Docker build
docker build -t api-gateway:latest .

# Docker run
docker run -d \
  -p 8090:8090 \
  -e REDIS_HOST=redis \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/myrealm \
  api-gateway:latest
```

---

## Next Steps

- [ ] Add circuit breaker (Resilience4j)
- [ ] Implement API key authentication
- [ ] Add request/response caching
- [ ] Implement request throttling per user
- [ ] Add API versioning support
- [ ] Implement request transformation
- [ ] Add WebSocket support
- [ ] Implement distributed tracing (OpenTelemetry)

---

**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 11 (API Gateway) Complete ✅

