# Cart Service

Shopping cart management service with inter-service communication to Catalog and Pricing services.

## ✅ Current Status (Dec 8, 2025)

**Latest Build:** 11:10:27 Dec 8, 2025  
**Port:** 8083 (changed from 8082/8085)  
**Status:** Production Ready ✅

### Recent Fixes Applied
- ✅ **CartRepository @Param Fix** - Added missing `@Param` annotations to query methods
- ✅ **Port Configuration** - Changed from 8085 to 8083
- ✅ **Pricing Endpoint** - Fixed to `/api/v1/pricing/price/{id}`
- ✅ **All Tests Passing** - Unit tests and contract tests working
- ✅ **Database Integration** - PostgreSQL connected and working

---

## Features

- ✅ **CRUD Operations**: Add, update, remove items from cart
- ✅ **WebClient Integration**: Non-blocking calls to Catalog and Pricing services
- ✅ **Session Management**: Support for both authenticated users and guest sessions
- ✅ **Price Enrichment**: Fetch current prices on cart view
- ✅ **Fallback Mechanism**: Use cached prices when pricing service is unavailable
- ✅ **Contract Tests**: WireMock-based tests for external service contracts
- ✅ **Resilience**: Retry logic and timeout handling
- ✅ **@Param Annotations**: Proper named parameter binding for JPA queries

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+

### 1. Clean Ports (IMPORTANT!)
```bash
# Kill any processes on required ports
for port in 8080 8081 8083; do
  lsof -ti:$port | xargs kill -9 2>/dev/null && echo "✅ Port $port cleared"
done
```

### 2. Start PostgreSQL
```bash
# Start existing container or create new one
docker start cartdb 2>/dev/null || docker run -d --name cartdb \
  -e POSTGRES_DB=cartdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

### 3. Start Mock Catalog (REQUIRED!)
```bash
cat > /tmp/mock_catalog.py << 'EOF'
from http.server import HTTPServer, BaseHTTPRequestHandler
import json

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if '/api/products/' in self.path:
            pid = self.path.split('/')[-1]
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({
                'id': pid, 'name': f'Product {pid}',
                'description': f'Test {pid}', 'category': 'Electronics',
                'basePrice': 99.99, 'available': True
            }).encode())
            print(f"✅ {pid}")
    def log_message(self, *args): pass
print("🚀 Mock Catalog on 8080"); HTTPServer(('', 8080), Handler).serve_forever()
EOF

python3 /tmp/mock_catalog.py
```

### 4. Start Mock Pricing (REQUIRED!)
```bash
cat > /tmp/mock_pricing.py << 'EOF'
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
from datetime import datetime

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if '/api/v1/pricing/price/' in self.path:
            pid = self.path.split('/')[-1]
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({
                'productId': pid, 'finalPrice': 89.99, 'basePrice': 99.99,
                'discount': 10.0, 'currency': 'USD',
                'effectiveDate': datetime.now().isoformat()
            }).encode())
            print(f"✅ {pid} -> $89.99")
    def log_message(self, *args): pass
print("🚀 Mock Pricing on 8081"); HTTPServer(('', 8081), Handler).serve_forever()
EOF

python3 /tmp/mock_pricing.py
```

### 5. Build & Run Cart Service
```bash
# Build (if needed)
cd services/cart-service/
mvn clean package -Dmaven.test.skip=true

# Run
java -jar target/cart-service-0.0.1-SNAPSHOT.jar
```

### 6. Verify
```bash
# Health check
curl http://localhost:8083/actuator/health | jq

# Add item to cart
curl -X POST http://localhost:8083/api/cart/items \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{"productId": "LAPTOP-001", "quantity": 2}' | jq
```

---

## Project Structure

### Directory Layout

```
ecommerce-microservices-platform/
└── services/
    └── cart-service/                    # ← You are here
        ├── README.md                    # This file
        ├── Dockerfile                   # Container configuration
        ├── pom.xml                      # Maven dependencies
        │
        ├── src/
        │   ├── main/
        │   │   ├── java/
        │   │   │   └── com/ecommerce/cart/
        │   │   │       ├── CartServiceApplication.java
        │   │   │       ├── controller/          # REST endpoints
        │   │   │       │   └── CartController.java
        │   │   │       ├── service/             # Business logic
        │   │   │       │   └── CartService.java
        │   │   │       ├── repository/          # Data access
        │   │   │       │   ├── CartRepository.java
        │   │   │       │   └── CartItemRepository.java
        │   │   │       ├── entity/              # JPA entities
        │   │   │       │   ├── Cart.java
        │   │   │       │   └── CartItem.java
        │   │   │       ├── dto/                 # Request/Response objects
        │   │   │       │   ├── CartResponse.java
        │   │   │       │   ├── CartItemResponse.java
        │   │   │       │   ├── AddItemRequest.java
        │   │   │       │   └── UpdateItemRequest.java
        │   │   │       ├── client/              # External service clients
        │   │   │       │   ├── CatalogServiceClient.java
        │   │   │       │   └── PricingServiceClient.java
        │   │   │       ├── exception/           # Custom exceptions
        │   │   │       │   ├── CartNotFoundException.java
        │   │   │       │   ├── ProductNotFoundException.java
        │   │   │       │   └── GlobalExceptionHandler.java
        │   │   │       └── config/              # Spring configuration
        │   │   │           ├── WebClientConfig.java
        │   │   │           └── OpenApiConfig.java
        │   │   │
        │   │   └── resources/
        │   │       ├── application.yml          # Configuration
        │   │       └── db/migration/            # Flyway SQL scripts
        │   │           ├── V1__create_carts_table.sql
        │   │           └── V2__create_cart_items_table.sql
        │   │
        │   └── test/
        │       └── java/
        │           └── com/ecommerce/cart/
        │               ├── service/
        │               │   └── CartServiceTest.java
        │               └── client/
        │                   ├── CatalogServiceContractTest.java
        │                   └── PricingServiceContractTest.java
        │
        ├── target/                              # Build output
        │   ├── cart-service-0.0.1-SNAPSHOT.jar
        │   └── classes/
        │
        └── scripts/                             # Utility scripts (if any)
```

### Navigation Commands

```bash
# From project root
cd services/cart-service

# View source code
cd src/main/java/com/ecommerce/cart

# View controllers
cd src/main/java/com/ecommerce/cart/controller

# View entities
cd src/main/java/com/ecommerce/cart/entity

# View tests
cd src/test/java/com/ecommerce/cart

# View configuration
cd src/main/resources

# View database migrations
cd src/main/resources/db/migration

# Back to cart-service root
cd /Users/narjeeshabdulkhadar/ecommerce-microservices-platform/services/cart-service

# View build artifacts
cd target
```

### Quick File Access

```bash
# Edit main configuration
vim src/main/resources/application.yml

# View main application class
cat src/main/java/com/ecommerce/cart/CartServiceApplication.java

# View REST controller
cat src/main/java/com/ecommerce/cart/controller/CartController.java

# View service logic
cat src/main/java/com/ecommerce/cart/service/CartService.java

# Check database schema
cat src/main/resources/db/migration/V1__create_carts_table.sql
```

---

### Inter-Service Communication

```
┌─────────────┐      WebClient      ┌─────────────────┐
│             │ ───────────────────> │ Catalog Service │
│   Cart      │                      │   (Port 8080)   │
│  Service    │                      └─────────────────┘
│ (Port 8082) │
│             │      WebClient      ┌─────────────────┐
│             │ ───────────────────> │ Pricing Service │
└─────────────┘                      │   (Port 8081)   │
                                     └─────────────────┘
```

### Data Model

```
Cart (1) ──── (N) CartItem
  - id (UUID)         - id (UUID)
  - userId            - productId
  - sessionId         - quantity
  - createdAt         - cachedPrice
  - updatedAt         - addedAt
```

## API Endpoints

### Get Cart
```bash
GET /api/cart
Headers:
  X-User-Id: user-123        # For authenticated users
  X-Session-Id: session-456  # For guest users (one required)

Response:
{
  "id": "cart-uuid",
  "userId": "user-123",
  "items": [
    {
      "id": "item-uuid",
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 2,
      "unitPrice": 899.99,
      "totalPrice": 1799.98,
      "priceAvailable": true
    }
  ],
  "totalItems": 2,
  "totalPrice": 1799.98
}
```

### Add Item
```bash
POST /api/cart/items
Headers:
  X-User-Id: user-123
Content-Type: application/json

{
  "productId": "PROD-001",
  "quantity": 2
}
```

### Update Item Quantity
```bash
PUT /api/cart/items/{itemId}
Content-Type: application/json

{
  "quantity": 3
}
```

### Remove Item
```bash
DELETE /api/cart/items/{itemId}
```

### Clear Cart
```bash
DELETE /api/cart/clear
Headers:
  X-User-Id: user-123
```

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cartdb
DB_USER=postgres
DB_PASSWORD=postgres

# Service URLs
CATALOG_SERVICE_URL=http://localhost:8080
PRICING_SERVICE_URL=http://localhost:8081

# Cart Settings
CART_MAX_ITEMS_PER_CART=50
CART_SESSION_TTL_DAYS=30

# Server
SERVER_PORT=8082
```

### WebClient Configuration

- **Connection Pool**: 100 max connections
- **Timeouts**: 
  - Catalog: 5 seconds
  - Pricing: 3 seconds
- **Retry Policy**: 3 attempts with exponential backoff

## Running the Service

### Local Development

```bash
# Build
mvn clean package

# Run with PostgreSQL
docker run -d --name cartdb \
  -e POSTGRES_DB=cartdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Start service
java -jar target/cart-service-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build image
docker build -t cart-service:latest .

# Run container
docker run -p 8082:8082 \
  -e DB_HOST=host.docker.internal \
  -e CATALOG_SERVICE_URL=http://host.docker.internal:8080 \
  -e PRICING_SERVICE_URL=http://host.docker.internal:8081 \
  cart-service:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  cart-service:
    build: .
    ports:
      - "8082:8082"
    environment:
      DB_HOST: cartdb
      CATALOG_SERVICE_URL: http://catalog-service:8080
      PRICING_SERVICE_URL: http://pricing-service:8081
    depends_on:
      - cartdb
  
  cartdb:
    image: postgres:15
    environment:
      POSTGRES_DB: cartdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
```

## Testing

### Run All Tests
```bash
mvn test
```

### Contract Tests
```bash
# Tests verify Catalog and Pricing service contracts
mvn test -Dtest=*ContractTest
```

### Unit Tests
```bash
mvn test -Dtest=CartServiceTest
```

## Key Learning Points

### 1. WebClient (Non-blocking HTTP)
- Uses Project Reactor (Mono/Flux)
- Configured with connection pooling
- Custom timeouts per service
- Error handling with fallbacks

### 2. Contract Testing
- WireMock stubs external services
- Validates API response structure
- Catches breaking changes early
- Independent of external service availability

### 3. Session Management
- Supports both user-based and session-based carts
- Database-backed for persistence
- Can merge carts on login (future)

### 4. Resilience Patterns
- **Retry**: 3 attempts with backoff
- **Timeout**: Prevent hanging requests
- **Fallback**: Use cached prices
- **Circuit Breaker**: (Future - Resilience4j)

## API Documentation

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8082/api-docs

## Monitoring

- **Health**: http://localhost:8083/actuator/health
- **Metrics**: http://localhost:8083/actuator/metrics
- **Prometheus**: http://localhost:8083/actuator/prometheus

## Week 5 Learning Summary

This Cart Service implementation demonstrates:

1. **WebClient (Non-blocking HTTP)**
   - Project Reactor (Mono/Flux)
   - Connection pooling and timeout configuration
   - Retry logic with exponential backoff
   - Error handling with fallbacks

2. **Contract Testing**
   - WireMock for external service simulation
   - API contract validation
   - Independent testing without external dependencies

3. **JPA Query Parameters**
   - `@Param` annotations for named parameters
   - JOIN FETCH for eager loading
   - Custom query methods

4. **Session Management**
   - User-based carts (authenticated)
   - Session-based carts (guest users)
   - Database persistence

5. **Resilience Patterns**
   - Retry mechanisms
   - Timeout handling
   - Fallback to cached data
   - Graceful error handling

6. **Docker & PostgreSQL**
   - Containerized database
   - Flyway migrations
   - Local development setup

---

## Next Steps (Week 6+)

- [ ] Event-driven cart updates (Kafka)
- [ ] Cart merge logic on user login
- [ ] Cart abandonment cleanup job
- [ ] Redis caching for frequently accessed carts
- [ ] GraphQL API for optimized queries
- [ ] Circuit breaker with Resilience4j

---

## Build & Test Commands

```bash
# Clean build
mvn clean package

# Run tests only
mvn test

# Run specific test
mvn test -Dtest=CartServiceTest

# Run contract tests
mvn test -Dtest=*ContractTest

# Skip tests
mvn clean package -Dmaven.test.skip=true

# Run with debug
mvn spring-boot:run -Ddebug

# Check code coverage
mvn jacoco:report
# Report: target/site/jacoco/index.html
```

---

## References

### Documentation Files (Cleaned Up)
All temporary setup and troubleshooting guides have been consolidated into this README.

### Key Configuration Files
- `pom.xml`: Maven dependencies and plugins
- `application.yml`: Service configuration
- `src/main/resources/db/migration/`: Flyway SQL scripts
- `Dockerfile`: Container image definition

### External Dependencies
- Spring Boot 3.1.6
- Spring WebFlux (WebClient)
- Spring Data JPA
- PostgreSQL Driver
- Flyway Migration
- WireMock (testing)
- Testcontainers (integration tests)

---

## Support & Issues

For issues or questions:
1. Check the Troubleshooting section above
2. Verify all mock services are running
3. Check logs: `target/logs/` or console output
4. Verify database connection: `curl http://localhost:8083/actuator/health`

---

**Last Updated:** December 8, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Production Ready ✅


