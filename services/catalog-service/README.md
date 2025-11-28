# Catalog Service

This is the Catalog Service for the E-Commerce Microservices Platform. It manages product catalog including items, categories, pricing, and inventory information.

## Service URL
- **Default Port**: `http://localhost:8081`
- **Actuator Health Check**: `http://localhost:8081/actuator/health`

## Prerequisites
- **Docker and Docker Compose** running
- **Java 17+** installed
- **Maven 3.6+** installed

---

## Build Commands

### 1. Build the JAR file (skip tests)
```bash
cd services/catalog-service
mvn clean package -DskipTests
```

### 2. Build with tests
```bash
cd services/catalog-service
mvn clean package
```

### 3. Quick build and deploy (one-liner)
```bash
cd services/catalog-service && mvn clean package -DskipTests && cd ../../infra && docker-compose up --build -d catalog-service
```

---

## Run Commands

### Option 1: Run with Docker Compose (Recommended)

#### Start all infrastructure + catalog service
```bash
cd infra
docker-compose up -d
```

#### Rebuild and restart catalog service only
```bash
cd infra
docker-compose up --build -d catalog-service
```

#### Stop all services
```bash
cd infra
docker-compose down
```

### Option 2: Run Locally without Docker

#### 1. Start only the database
```bash
cd infra
docker-compose up -d catalog-postgres
```

#### 2. Run the application using Maven
```bash
cd services/catalog-service
mvn spring-boot:run
```

#### 3. Run the JAR directly
```bash
cd services/catalog-service
java -jar target/catalog-service-0.0.1-SNAPSHOT.jar
```

---

## Testing

### Run all tests (unit + integration + service tests)
```bash
cd services/catalog-service
mvn clean test
```

**Test Summary**: 34 total tests
- 15 unit tests (CatalogControllerTest)
- 7 integration tests (CatalogControllerIntegrationTest)
- 12 service tests (ItemServiceTest)

### Run specific test classes

#### Run only unit tests
```bash
cd services/catalog-service
mvn test -Dtest=CatalogControllerTest
```

#### Run only integration tests
```bash
cd services/catalog-service
mvn test -Dtest=CatalogControllerIntegrationTest
```

#### Run only service tests
```bash
cd services/catalog-service
mvn test -Dtest=ItemServiceTest
```

### Run specific test methods
```bash
# Run a single test method
mvn test -Dtest=CatalogControllerTest#shouldCreateItem_WithValidData

# Run multiple test methods
mvn test -Dtest=CatalogControllerTest#shouldCreateItem_WithValidData+shouldGetAllItems
```

### Run tests with verbose output
```bash
cd services/catalog-service
mvn test -X
```

### Run tests and skip compilation
```bash
cd services/catalog-service
mvn surefire:test
```

**Note**: 
- Integration tests use **Testcontainers** to spin up a PostgreSQL database automatically. No manual database setup is needed.
- Unit tests use mocked dependencies and run very fast (~1 second).
- Integration tests take longer (~10-15 seconds) as they start a real database container.

### Test with coverage (if JaCoCo is configured)
```bash
cd services/catalog-service
mvn clean test jacoco:report
```

### View test reports
After running tests, view the HTML reports at:
```
services/catalog-service/target/surefire-reports/
```

---

## Test Coverage

### Unit Tests (CatalogControllerTest)
Fast, isolated controller tests using mocked dependencies.

**Tests (15 total)**:
- ✅ `shouldGetAllItems` - Verify GET all items endpoint
- ✅ `shouldGetItemById_WhenItemExists` - Verify GET by ID returns item
- ✅ `shouldReturn404_WhenItemNotFound` - Verify 404 for missing item
- ✅ `shouldCreateItem_WithValidData` - Verify POST creates item successfully
- ✅ `shouldReturn400_WhenCreatingItemWithMissingName` - Verify validation rejects missing name
- ✅ `shouldReturn400_WhenCreatingItemWithNegativePrice` - Verify validation rejects negative price
- ✅ `shouldReturn400_WhenCreatingItemWithInvalidJson` - Verify malformed JSON returns 400
- ✅ `shouldUpdateItem_WhenItemExists` - Verify PUT updates item
- ✅ `shouldReturn404_WhenUpdatingNonExistentItem` - Verify 404 when updating missing item
- ✅ `shouldDeleteItem` - Verify DELETE removes item
- ✅ `shouldSearchItems_WithNameAndCategory` - Verify search with multiple filters
- ✅ `shouldSearchItems_WithOnlyName` - Verify search with single filter
- ✅ `shouldReturnEmptyResults_WhenSearchFindsNothing` - Verify empty results handling
- ✅ `shouldHandleDefaultPaginationParameters` - Verify default pagination
- ✅ `shouldReturn400_WhenInvalidPageNumberProvided` - Verify invalid pagination handling

**Key Features**:
- Uses `@WebMvcTest` for lightweight testing
- Mocks `ItemService` and `ModelMapper`
- Security filters disabled for easier testing
- Runs in ~1 second

### Integration Tests (CatalogControllerIntegrationTest)
Full end-to-end tests with real database using Testcontainers.

**Tests (7 total)**:
- ✅ Full CRUD operations with PostgreSQL
- ✅ Database transactions and rollbacks
- ✅ Flyway migrations
- ✅ Search with pagination
- ✅ Real HTTP requests via MockMvc

**Key Features**:
- Uses `@SpringBootTest` with full context
- Real PostgreSQL database via Testcontainers
- Tests actual database queries and persistence
- Runs in ~10-15 seconds

### Service Tests (ItemServiceTest)
Business logic tests with mocked repository.

**Tests (12 total)**:
- ✅ Service layer business logic
- ✅ Repository interaction
- ✅ Exception handling
- ✅ Data transformation

**Key Features**:
- Unit tests for service layer
- Mocked `ItemRepository`
- Fast execution (~300ms)

---

## Verify Service is Running

### Check Docker logs
```bash
docker logs catalog-service -f
```

### Check health endpoint
```bash
curl http://localhost:8081/actuator/health
```

### Test API endpoints

#### Search all items (paginated)
```bash
curl "http://localhost:8081/catalog/items/search?page=0&size=10"
```

#### Get item by ID
```bash
curl http://localhost:8081/catalog/items/1
```

#### Search by category
```bash
curl "http://localhost:8081/catalog/items/search?category=Electronics&page=0&size=10"
```

---

## Authentication & Security

### Local Development (Permissive Mode)

For local development, security is temporarily disabled to allow easy testing via Postman or curl without authentication.

**Important**: This is only for local development. Do not use this configuration in CI/CD or production environments.

### Production (Keycloak JWT Authentication)

In production, the service uses OAuth2 JWT tokens from Keycloak.

#### Enable Keycloak JWT validation

Set the `issuer-uri` to your realm:

```bash
java -jar target/catalog-service-0.0.1-SNAPSHOT.jar \
  --server.port=8081 \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/myrealm
```

#### Get an access token (local Keycloak)

If you have Keycloak running locally, use the helper script:

```bash
chmod +x infra/keycloak/get-token.sh
infra/keycloak/get-token.sh myrealm myclient testuser password
```

#### Call the API with the token

```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:8081/catalog/items
```

---

## API Endpoints

### Public Endpoints (Local Dev Only)
- `GET /catalog/items/search` - Search items with optional filters (name, category, pagination)
- `GET /catalog/items/{id}` - Get item by ID
- `POST /catalog/items` - Create a new item
- `PUT /catalog/items/{id}` - Update an item
- `DELETE /catalog/items/{id}` - Delete an item

### Actuator Endpoints
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Service info

---

## Example Requests

### 1. Create an Item (POST)

```bash
curl -X POST http://localhost:8081/catalog/items \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "Dell XPS 13",
    "description": "13-inch laptop with Intel Core i7 processor",
    "price": 1299.99,
    "quantity": 50,
    "category": "Electronics"
  }'
```

**Expected Response (201 Created)**:
```json
{
  "id": 1,
  "sku": "LAPTOP-001",
  "name": "Dell XPS 13",
  "description": "13-inch laptop with Intel Core i7 processor",
  "price": 1299.99,
  "quantity": 50,
  "category": "Electronics",
  "createdAt": "2025-11-28T10:30:00Z",
  "updatedAt": "2025-11-28T10:30:00Z"
}
```

### 2. Get All Items (GET with Pagination)

```bash
curl "http://localhost:8081/catalog/items/search?page=0&size=10"
```

**Expected Response (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "sku": "LAPTOP-001",
      "name": "Dell XPS 13",
      "description": "13-inch laptop with Intel Core i7 processor",
      "price": 1299.99,
      "quantity": 50,
      "category": "Electronics",
      "createdAt": "2025-11-28T10:30:00Z",
      "updatedAt": "2025-11-28T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

### 3. Search Items by Category

```bash
curl "http://localhost:8081/catalog/items/search?category=Electronics&page=0&size=10"
```

### 4. Search Items by Name

```bash
curl "http://localhost:8081/catalog/items/search?name=Dell&page=0&size=10"
```

### 5. Search Items by Name and Category

```bash
curl "http://localhost:8081/catalog/items/search?name=Dell&category=Electronics&page=0&size=10"
```

### 6. Get Item by ID

```bash
curl http://localhost:8081/catalog/items/1
```

**Expected Response (200 OK)**:
```json
{
  "id": 1,
  "sku": "LAPTOP-001",
  "name": "Dell XPS 13",
  "description": "13-inch laptop with Intel Core i7 processor",
  "price": 1299.99,
  "quantity": 50,
  "category": "Electronics",
  "createdAt": "2025-11-28T10:30:00Z",
  "updatedAt": "2025-11-28T10:30:00Z"
}
```

**Expected Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-28T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Item not found with id: 999",
  "path": "/catalog/items/999"
}
```

### 7. Update an Item (PUT)

```bash
curl -X PUT http://localhost:8081/catalog/items/1 \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "Dell XPS 13 (Updated)",
    "description": "13-inch laptop with Intel Core i7 processor - 16GB RAM",
    "price": 1399.99,
    "quantity": 45,
    "category": "Electronics"
  }'
```

**Expected Response (200 OK)**:
```json
{
  "id": 1,
  "sku": "LAPTOP-001",
  "name": "Dell XPS 13 (Updated)",
  "description": "13-inch laptop with Intel Core i7 processor - 16GB RAM",
  "price": 1399.99,
  "quantity": 45,
  "category": "Electronics",
  "createdAt": "2025-11-28T10:30:00Z",
  "updatedAt": "2025-11-28T10:45:00Z"
}
```

### 8. Delete an Item (DELETE)

```bash
curl -X DELETE http://localhost:8081/catalog/items/1
```

**Expected Response (204 No Content)**: Empty response body

### 9. Validation Error Examples

#### Missing required field (name)
```bash
curl -X POST http://localhost:8081/catalog/items \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-002",
    "price": 1299.99,
    "quantity": 50,
    "category": "Electronics"
  }'
```

**Expected Response (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-28T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Name is required",
  "path": "/catalog/items"
}
```

#### Negative price
```bash
curl -X POST http://localhost:8081/catalog/items \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-003",
    "name": "Invalid Item",
    "price": -99.99,
    "quantity": 50,
    "category": "Electronics"
  }'
```

**Expected Response (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-28T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Price must be positive",
  "path": "/catalog/items"
}
```

### 10. Health Check

```bash
curl http://localhost:8081/actuator/health
```

**Expected Response (200 OK)**:
```json
{
  "status": "UP"
}
```

---

## OpenAPI Specification

The complete OpenAPI specification is available at:
```
docs/api/catalog.yaml
```

You can view and test the API using Swagger UI (if enabled) or import the spec into Postman.

---

## Troubleshooting

### Build & Runtime Issues

#### Issue: "no main manifest attribute, in /app/app.jar"

**Cause**: The JAR file was not built correctly or doesn't contain the Spring Boot manifest.

**Solution**: Rebuild the project with Maven:
```bash
cd services/catalog-service
mvn clean package -DskipTests
```

#### Issue: Catalog service container keeps restarting

**Check logs**:
```bash
docker logs catalog-service -f
```

**Common causes**:
1. Database not ready yet (wait a few seconds)
2. JAR file not built
3. Port 8081 already in use

#### Issue: Cannot connect to database

Ensure the PostgreSQL container is running:
```bash
docker ps | grep catalog-postgres
```

Restart if needed:
```bash
cd infra
docker-compose restart catalog-postgres catalog-service
```

### Test Issues

#### Issue: "NoSuchBeanDefinitionException: No qualifying bean of type 'org.modelmapper.ModelMapper'"

**Cause**: Unit test context doesn't include ModelMapper bean.

**Solution**: This is already fixed in `CatalogControllerTest.java` with `@MockBean` annotation. If you see this error, ensure you have:
```java
@MockBean
private org.modelmapper.ModelMapper modelMapper;
```

#### Issue: Tests fail with "401 Unauthorized" or "403 Forbidden"

**Cause**: Spring Security filters are blocking test requests.

**Solution**: Already fixed with `@AutoConfigureMockMvc(addFilters = false)`. Verify this annotation is present:
```java
@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {}
```

#### Issue: Integration tests fail with "Could not start container"

**Cause**: Docker is not running or Testcontainers cannot access Docker daemon.

**Solution**:
1. Ensure Docker Desktop is running
2. Check Docker is accessible: `docker ps`
3. Grant Docker socket permissions if on Linux: `sudo chmod 666 /var/run/docker.sock`

#### Issue: Tests fail with validation errors unexpectedly

**Cause**: Missing validation dependency or annotations.

**Solution**: Ensure `spring-boot-starter-validation` is in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

And DTOs have validation annotations:
```java
@NotBlank(message = "Name is required")
private String name;

@Positive(message = "Price must be positive")
private Double price;
```

#### Issue: Test coverage reports not generated

**Solution**: Add JaCoCo plugin to `pom.xml` and run:
```bash
mvn clean test jacoco:report
```

View report at: `target/site/jacoco/index.html`

---

## Notes

- The `local` profile is intentionally permissive for developer convenience. **Do not enable it in CI or production.**
- The app will automatically enable OAuth2 Resource Server config when `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set.
- Integration tests use **Testcontainers** to manage test databases automatically.
- For Postman testing, authentication is disabled in local development mode.

---

## Database Schema

The service uses **Flyway** for database migrations. Migration scripts are located in:
```
src/main/resources/db/migration/
```

Database table: `item`
- `id` (BIGINT, primary key)
- `sku` (VARCHAR, unique)
- `name` (VARCHAR)
- `description` (TEXT)
- `price` (NUMERIC)
- `quantity` (INT)
- `category` (VARCHAR)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
# CI/CD Pipeline Test

<!-- CI trigger: 2025-11-28 08:45:37 -->

<!-- CI trigger: 2025-11-28 10:47:52 -->
