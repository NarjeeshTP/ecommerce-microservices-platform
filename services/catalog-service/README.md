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

### Run integration tests
```bash
cd services/catalog-service
mvn clean test
```

**Note**: Integration tests use **Testcontainers** to spin up a PostgreSQL database automatically. No manual database setup is needed.

### Test with coverage (if JaCoCo is configured)
```bash
cd services/catalog-service
mvn clean test jacoco:report
```

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

## Troubleshooting

### Issue: "no main manifest attribute, in /app/app.jar"

**Cause**: The JAR file was not built correctly or doesn't contain the Spring Boot manifest.

**Solution**: Rebuild the project with Maven:
```bash
cd services/catalog-service
mvn clean package -DskipTests
```

### Issue: Catalog service container keeps restarting

**Check logs**:
```bash
docker logs catalog-service -f
```

**Common causes**:
1. Database not ready yet (wait a few seconds)
2. JAR file not built
3. Port 8081 already in use

### Issue: Cannot connect to database

Ensure the PostgreSQL container is running:
```bash
docker ps | grep catalog-postgres
```

Restart if needed:
```bash
cd infra
docker-compose restart catalog-postgres catalog-service
```

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
