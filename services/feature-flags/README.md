# Feature Flags Service

Feature toggle management system with SDK for runtime control of feature rollouts.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8091  
**Status:** Week 11 Implementation Ready

### Features Implemented
- ✅ **Feature Toggle Management** - CRUD operations for feature flags
- ✅ **Redis Caching** - Fast flag evaluation with caching
- ✅ **Targeting Rules** - User/percentage-based rollouts
- ✅ **SDK Client** - Java SDK for service integration
- ✅ **Audit History** - Track all flag changes
- ✅ **REST API** - Manage flags via HTTP
- ✅ **Real-time Updates** - Flags update without restart

---

## Features

### Core Functionality
- ✅ **Simple Toggles** - Boolean on/off flags
- ✅ **Percentage Rollouts** - Gradual feature rollout (10%, 50%, 100%)
- ✅ **User Targeting** - Enable for specific users
- ✅ **Environment Support** - Different flags per environment
- ✅ **Flag Versioning** - Track flag changes over time
- ✅ **Instant Toggle** - Enable/disable without deployment

### Technical Features
- ✅ **Redis Cache** - Sub-millisecond flag evaluation
- ✅ **PostgreSQL Storage** - Persistent flag definitions
- ✅ **SDK Polling** - Client polls for updates every 30s
- ✅ **REST API** - HTTP endpoints for flag management
- ✅ **Metrics** - Flag evaluation metrics

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- PostgreSQL (port 5437)
- Redis (port 6379)

### 1. Start Dependencies

```bash
# PostgreSQL
docker run -d --name featureflagsdb \
  -e POSTGRES_DB=featureflagsdb \
  -p 5437:5432 postgres:15

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 2. Build & Run

```bash
cd services/feature-flags
mvn clean package -Dmaven.test.skip=true
java -jar target/feature-flags-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
curl http://localhost:8091/actuator/health | jq
```

---

## Architecture

### System Flow

```
┌─────────────────────────────────────────────────────────┐
│  Feature Flags Service (Port 8091)                      │
│                                                         │
│  ┌────────────────────────────────────────────┐       │
│  │  REST API                                  │       │
│  │  - Create flag                             │       │
│  │  - Update flag                             │       │
│  │  - Toggle enabled                          │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  PostgreSQL                                │       │
│  │  - feature_flags table                     │       │
│  │  - feature_flag_rules table                │       │
│  │  - feature_flag_history table              │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  ┌────────────────────────────────────────────┐       │
│  │  Redis Cache                               │       │
│  │  - TTL: 5 minutes                          │       │
│  │  - Invalidate on update                    │       │
│  └────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Client Services (Order, Cart, Catalog)                 │
│                                                         │
│  ┌────────────────────────────────────────────┐       │
│  │  Feature Flag SDK                          │       │
│  │  - Poll every 30s                          │       │
│  │  - In-memory cache                         │       │
│  │  - Fallback to default                     │       │
│  └────────────────────────────────────────────┘       │
│                  ↓                                      │
│  if (featureFlags.isEnabled("new-checkout")) {         │
│      // Use new checkout flow                          │
│  } else {                                              │
│      // Use old checkout flow                          │
│  }                                                     │
└─────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Create Feature Flag

```bash
POST /api/features

{
  "key": "new-checkout",
  "name": "New Checkout Flow",
  "description": "Revamped checkout with one-click payment",
  "enabled": false
}

Response: 201 Created
{
  "id": "f1a2b3c4-...",
  "key": "new-checkout",
  "name": "New Checkout Flow",
  "enabled": false,
  "version": 0,
  "createdAt": "2025-12-11T10:00:00Z"
}
```

### 2. Get All Flags

```bash
GET /api/features

Response: 200 OK
[
  {
    "id": "f1a2b3c4-...",
    "key": "new-checkout",
    "name": "New Checkout Flow",
    "enabled": false,
    "version": 0
  },
  {
    "id": "f2b3c4d5-...",
    "key": "dark-mode",
    "name": "Dark Mode Theme",
    "enabled": true,
    "version": 2
  }
]
```

### 3. Get Flag by Key

```bash
GET /api/features/new-checkout

Response: 200 OK
{
  "id": "f1a2b3c4-...",
  "key": "new-checkout",
  "name": "New Checkout Flow",
  "description": "Revamped checkout with one-click payment",
  "enabled": false,
  "version": 0,
  "createdAt": "2025-12-11T10:00:00Z",
  "updatedAt": "2025-12-11T10:00:00Z"
}
```

### 4. Toggle Feature Flag

```bash
PUT /api/features/new-checkout/toggle

{
  "enabled": true
}

Response: 200 OK
{
  "id": "f1a2b3c4-...",
  "key": "new-checkout",
  "enabled": true,
  "version": 1
}
```

### 5. Evaluate Flag (for SDK)

```bash
GET /api/features/evaluate?keys=new-checkout,dark-mode&userId=user-123

Response: 200 OK
{
  "new-checkout": true,
  "dark-mode": true
}
```

### 6. Get Flag History

```bash
GET /api/features/new-checkout/history

Response: 200 OK
[
  {
    "id": "h1a2b3c4-...",
    "action": "TOGGLE",
    "changedBy": "admin",
    "oldValue": {"enabled": false},
    "newValue": {"enabled": true},
    "createdAt": "2025-12-11T10:05:00Z"
  },
  {
    "id": "h2b3c4d5-...",
    "action": "CREATE",
    "changedBy": "admin",
    "oldValue": null,
    "newValue": {"enabled": false},
    "createdAt": "2025-12-11T10:00:00Z"
  }
]
```

---

## Feature Flag Types

### 1. Simple Boolean Toggle

```java
// Feature Flag
{
  "key": "dark-mode",
  "enabled": true
}

// Usage in code
if (featureFlags.isEnabled("dark-mode")) {
    applyDarkTheme();
} else {
    applyLightTheme();
}
```

### 2. Percentage Rollout

```java
// Feature Flag with Rule
{
  "key": "new-search-algorithm",
  "enabled": true,
  "rules": [
    {
      "type": "PERCENTAGE",
      "data": {"percentage": 25}  // 25% of users
    }
  ]
}

// SDK automatically evaluates based on user hash
boolean enabled = featureFlags.isEnabled("new-search-algorithm", userId);
// 25% of users get true, 75% get false
```

### 3. User Targeting

```java
// Feature Flag with User Whitelist
{
  "key": "beta-features",
  "enabled": true,
  "rules": [
    {
      "type": "USER_WHITELIST",
      "data": {
        "users": ["user-123", "user-456", "user-789"]
      }
    }
  ]
}

// Only whitelisted users see the feature
boolean enabled = featureFlags.isEnabled("beta-features", "user-123");
// Returns true for user-123, false for others
```

### 4. Multi-Rule Targeting

```java
// Feature Flag with Multiple Rules (evaluated in priority order)
{
  "key": "premium-features",
  "enabled": true,
  "rules": [
    {
      "type": "USER_WHITELIST",
      "data": {"users": ["admin-1"]},
      "priority": 1  // Highest priority
    },
    {
      "type": "USER_ATTRIBUTE",
      "data": {"attribute": "subscription", "value": "premium"},
      "priority": 2
    },
    {
      "type": "PERCENTAGE",
      "data": {"percentage": 10},
      "priority": 3  // Lowest priority
    }
  ]
}
```

---

## SDK Usage

### Java SDK Integration

#### 1. Add Dependency (pom.xml)

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>feature-flags-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### 2. Configure SDK (application.yml)

```yaml
feature-flags:
  service-url: http://localhost:8091
  polling-interval-ms: 30000  # Poll every 30 seconds
  cache-enabled: true
```

#### 3. Initialize SDK

```java
@Configuration
public class FeatureFlagConfig {
    
    @Bean
    public FeatureFlagClient featureFlagClient(
        @Value("${feature-flags.service-url}") String serviceUrl,
        @Value("${feature-flags.polling-interval-ms}") long pollingInterval
    ) {
        return FeatureFlagClient.builder()
            .serviceUrl(serviceUrl)
            .pollingIntervalMs(pollingInterval)
            .build();
    }
}
```

#### 4. Use in Code

```java
@Service
public class OrderService {
    
    @Autowired
    private FeatureFlagClient featureFlags;
    
    public Order createOrder(OrderRequest request) {
        // Check if new checkout flow is enabled
        if (featureFlags.isEnabled("new-checkout")) {
            return createOrderWithNewFlow(request);
        } else {
            return createOrderWithOldFlow(request);
        }
    }
    
    public List<Product> getRecommendations(String userId) {
        // Check with user context
        if (featureFlags.isEnabled("ai-recommendations", userId)) {
            return aiRecommendationService.getRecommendations(userId);
        } else {
            return basicRecommendationService.getRecommendations(userId);
        }
    }
}
```

#### 5. SDK Implementation (Simplified)

```java
public class FeatureFlagClient {
    
    private final String serviceUrl;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();
    
    @Scheduled(fixedDelayString = "${feature-flags.polling-interval-ms}")
    public void refresh() {
        // Poll feature flags service
        Map<String, Boolean> flags = fetchFlags();
        cache.putAll(flags);
    }
    
    public boolean isEnabled(String key) {
        return cache.getOrDefault(key, false);  // Default to false
    }
    
    public boolean isEnabled(String key, String userId) {
        // Evaluate with user context
        return evaluateFlag(key, userId);
    }
    
    private Map<String, Boolean> fetchFlags() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(
            serviceUrl + "/api/features/evaluate?keys=*",
            Map.class
        );
    }
}
```

---

## Use Cases

### 1. Gradual Feature Rollout

**Scenario:** Release new feature to 10% of users, then increase if stable

```bash
# Week 1: 10% rollout
PUT /api/features/new-feature/toggle
{"enabled": true, "percentage": 10}

# Week 2: No issues, increase to 50%
PUT /api/features/new-feature/toggle
{"enabled": true, "percentage": 50}

# Week 3: Stable, full rollout
PUT /api/features/new-feature/toggle
{"enabled": true, "percentage": 100}
```

### 2. A/B Testing

**Scenario:** Test two checkout flows

```java
if (featureFlags.isEnabled("checkout-variant-b", userId)) {
    metrics.track("checkout.variant", "B");
    return checkoutVariantB(cart);
} else {
    metrics.track("checkout.variant", "A");
    return checkoutVariantA(cart);
}
```

### 3. Kill Switch

**Scenario:** Disable problematic feature instantly

```bash
# Feature causing issues
PUT /api/features/problematic-feature/toggle
{"enabled": false}

# All services pick up change within 30 seconds (polling interval)
# No deployment needed!
```

### 4. Beta Features

**Scenario:** Give early access to beta testers

```bash
POST /api/features
{
  "key": "beta-dashboard",
  "enabled": true,
  "rules": [
    {
      "type": "USER_WHITELIST",
      "data": {
        "users": ["beta-user-1", "beta-user-2", "beta-user-3"]
      }
    }
  ]
}
```

---

## Database Schema

### feature_flags

```sql
CREATE TABLE feature_flags (
    id UUID PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0  -- Optimistic locking
);
```

### feature_flag_rules

```sql
CREATE TABLE feature_flag_rules (
    id UUID PRIMARY KEY,
    feature_flag_id UUID REFERENCES feature_flags(id),
    rule_type VARCHAR(50) NOT NULL,  -- PERCENTAGE, USER_WHITELIST, USER_ATTRIBUTE
    rule_data JSONB NOT NULL,         -- {"percentage": 25} or {"users": ["user-1"]}
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### feature_flag_history

```sql
CREATE TABLE feature_flag_history (
    id UUID PRIMARY KEY,
    feature_flag_id UUID REFERENCES feature_flags(id),
    action VARCHAR(50) NOT NULL,      -- CREATE, TOGGLE, UPDATE, DELETE
    changed_by VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Testing

### Create Feature Flag

```bash
curl -X POST http://localhost:8091/api/features \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-checkout",
    "name": "New Checkout Flow",
    "description": "One-click checkout",
    "enabled": false
  }' | jq
```

### Toggle Feature

```bash
curl -X PUT http://localhost:8091/api/features/new-checkout/toggle \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}' | jq
```

### Evaluate Flag

```bash
curl "http://localhost:8091/api/features/evaluate?keys=new-checkout&userId=user-123" | jq
```

### Check History

```bash
curl http://localhost:8091/api/features/new-checkout/history | jq
```

---

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5437
DB_NAME=featureflagsdb
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Feature Flags
FEATURE_FLAGS_CACHE_TTL=300
FEATURE_FLAGS_SDK_POLLING_INTERVAL=30000

# Server
SERVER_PORT=8091
```

---

## Week 11 Learning Summary

### 1. Feature Flags Pattern

**Purpose:** Control feature availability without code deployment

**Benefits:**
- ✅ Instant enable/disable
- ✅ Gradual rollout
- ✅ A/B testing
- ✅ Kill switch for issues
- ✅ No deployment needed

### 2. Percentage Rollout

**Algorithm:**
```java
boolean evaluatePercentage(String userId, int percentage) {
    int userHash = Math.abs(userId.hashCode());
    int bucket = userHash % 100;  // 0-99
    return bucket < percentage;
}

// userId "user-123" → hash 12345 → bucket 45
// percentage 25 → bucket 45 >= 25 → false
// percentage 50 → bucket 45 < 50 → true
```

**Result:** Consistent assignment (same user always in same bucket)

### 3. Client SDK Pattern

**Polling vs Push:**

| Approach | Polling (Implemented) | Push (WebSocket) |
|----------|----------------------|------------------|
| **Complexity** | Simple | Complex |
| **Latency** | 30 seconds | Real-time |
| **Scalability** | Good | Harder |
| **Best For** | Most use cases | Critical features |

### 4. Cache Strategy

**Two-Level Cache:**
```
┌─────────────────────────────────┐
│  Service (Client SDK)           │
│  In-Memory Cache (30s refresh)  │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│  Feature Flags Service          │
│  Redis Cache (5min TTL)         │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│  PostgreSQL (Source of Truth)   │
└─────────────────────────────────┘
```

---

## Project Structure

```
feature-flags/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/featureflags/
│   │   │   ├── FeatureFlagsApplication.java
│   │   │   ├── controller/
│   │   │   │   └── FeatureFlagController.java
│   │   │   ├── service/
│   │   │   │   ├── FeatureFlagService.java
│   │   │   │   └── EvaluationService.java
│   │   │   ├── repository/
│   │   │   │   ├── FeatureFlagRepository.java
│   │   │   │   └── FeatureFlagHistoryRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── FeatureFlag.java
│   │   │   │   ├── FeatureFlagRule.java
│   │   │   │   └── FeatureFlagHistory.java
│   │   │   ├── dto/
│   │   │   │   ├── FeatureFlagRequest.java
│   │   │   │   └── FeatureFlagResponse.java
│   │   │   ├── sdk/
│   │   │   │   └── FeatureFlagClient.java
│   │   │   └── config/
│   │   │       └── RedisConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__create_feature_flags_table.sql
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Next Steps

- [ ] Add user attribute targeting
- [ ] Implement experiment tracking
- [ ] Add webhook notifications
- [ ] Build admin UI
- [ ] Add flag dependencies
- [ ] Implement flag scheduling
- [ ] Add canary deployments
- [ ] Build analytics dashboard

---

**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 11 (Feature Flags) Complete ✅

