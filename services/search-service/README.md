# Search Service

Product search service with Elasticsearch indexing and full-text search capabilities.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.0.1-SNAPSHOT  
**Port:** 8088  
**Status:** Week 9 Implementation Ready

### Features Implemented
- ✅ **Elasticsearch Integration** - Full-text search engine
- ✅ **Product Indexing** - Index catalog products
- ✅ **Search API** - Query products by keywords
- ✅ **Autocomplete** - Search suggestions
- ✅ **Faceted Search** - Filter by category, price, brand
- ✅ **Event-Driven Sync** - Listen to catalog changes
- ✅ **Bulk Indexing** - Initial data sync from Catalog Service
- ✅ **Real-Time Updates** - Immediate index updates on catalog changes

---

## Features

### Core Functionality
- ✅ **Full-Text Search** - Search product name, description, tags
- ✅ **Autocomplete** - Type-ahead suggestions
- ✅ **Filters** - Category, price range, brand, rating
- ✅ **Sorting** - Relevance, price, rating, newest
- ✅ **Pagination** - Efficient result pagination
- ✅ **Fuzzy Search** - Typo tolerance
- ✅ **Boosting** - Weighted fields (name > description)

### Technical Features
- ✅ **Elasticsearch 8.x** - Modern search engine
- ✅ **Kafka Consumer** - Listen to catalog-events
- ✅ **Bulk Sync** - Initial sync from Catalog Service
- ✅ **WebClient** - Non-blocking Catalog API calls
- ✅ **Index Management** - Create/update/delete indices
- ✅ **Query DSL** - Complex search queries
- ✅ **Aggregations** - Facets and analytics

---

## Quick Start

### Prerequisites
- Docker Desktop running
- Java 17+
- Maven 3.6+
- Elasticsearch (port 9200)
- Kafka (port 9092)

### 1. Start Elasticsearch

```bash
docker run -d --name elasticsearch \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -p 9200:9200 \
  -p 9300:9300 \
  elasticsearch:8.11.0
```

### 2. Build & Run

```bash
cd services/search-service
mvn clean package -Dmaven.test.skip=true
java -jar target/search-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify

```bash
# Health check
curl http://localhost:8088/actuator/health | jq

# Elasticsearch health
curl http://localhost:9200/_cluster/health | jq
```

---

## Search Flow

### Architecture Diagram

```
┌──────────────┐
│ Catalog      │
│ Service      │
└──────┬───────┘
       │
       │ 1. ProductCreated/Updated event
       │
       ↓
┌──────────────────────────────────┐
│  Kafka (catalog-events topic)    │
└──────┬───────────────────────────┘
       │
       │ 2. Event consumed
       │
       ↓
┌──────────────────────────────────┐
│  Search Service                  │
│  - CatalogEventListener          │
└──────┬───────────────────────────┘
       │
       │ 3. Index product
       │
       ↓
┌──────────────────────────────────┐
│  Elasticsearch                   │
│  - products index                │
└──────┬───────────────────────────┘
       │
       ↓
┌──────────────────────────────────┐
│  User searches                   │
│  GET /api/search?q=laptop        │
└──────┬───────────────────────────┘
       │
       ↓
┌──────────────────────────────────┐
│  Search results returned         │
└──────────────────────────────────┘
```

### Data Sync Strategies

#### 1. Initial Sync (Startup)

```
Service Startup
    ↓
Fetch all products from Catalog Service
    ↓
Bulk index to Elasticsearch (batches of 100)
    ↓
Ready for search queries
```

#### 2. Real-Time Sync (Events)

```
Catalog Service → Product updated
    ↓
Publish ProductUpdated event to Kafka
    ↓
Search Service consumes event
    ↓
Update Elasticsearch index immediately
```

---

## API Endpoints

### 1. Search Products

```bash
GET /api/search?q=laptop&category=electronics&minPrice=500&maxPrice=2000&page=0&size=20

Response: 200 OK
{
  "query": "laptop",
  "totalHits": 47,
  "page": 0,
  "size": 20,
  "results": [
    {
      "id": "LAPTOP-001",
      "name": "MacBook Pro 16-inch",
      "description": "Powerful laptop with M3 chip",
      "price": 2499.99,
      "category": "electronics",
      "brand": "Apple",
      "rating": 4.8,
      "inStock": true,
      "imageUrl": "https://cdn.example.com/macbook.jpg",
      "score": 12.5  // Relevance score
    },
    {
      "id": "LAPTOP-002",
      "name": "Dell XPS 15",
      "description": "Premium laptop for professionals",
      "price": 1799.99,
      "category": "electronics",
      "brand": "Dell",
      "rating": 4.6,
      "inStock": true,
      "score": 11.2
    }
  ],
  "facets": {
    "categories": {
      "electronics": 47,
      "computers": 32
    },
    "brands": {
      "Apple": 12,
      "Dell": 15,
      "HP": 10,
      "Lenovo": 10
    },
    "priceRanges": {
      "0-500": 5,
      "500-1000": 12,
      "1000-2000": 18,
      "2000+": 12
    }
  }
}
```

**Query Parameters:**
- `q` - Search query (required)
- `category` - Filter by category
- `brand` - Filter by brand
- `minPrice`, `maxPrice` - Price range
- `minRating` - Minimum rating
- `inStock` - Only in-stock items
- `sort` - Sort by: `relevance`, `price_asc`, `price_desc`, `rating`, `newest`
- `page`, `size` - Pagination

### 2. Autocomplete

```bash
GET /api/search/suggest?q=lap

Response: 200 OK
{
  "query": "lap",
  "suggestions": [
    "laptop",
    "laptop bag",
    "laptop stand",
    "laptop charger",
    "laptop sleeve"
  ]
}
```

### 3. Get Product by ID (from Search)

```bash
GET /api/search/products/{productId}

Response: 200 OK
{
  "id": "LAPTOP-001",
  "name": "MacBook Pro 16-inch",
  "description": "Powerful laptop with M3 chip",
  "price": 2499.99,
  "category": "electronics",
  "brand": "Apple",
  "rating": 4.8,
  "inStock": true
}
```

### 4. Similar Products

```bash
GET /api/search/products/{productId}/similar

Response: 200 OK
[
  {
    "id": "LAPTOP-003",
    "name": "MacBook Air",
    "similarity": 0.85
  },
  {
    "id": "LAPTOP-004",
    "name": "Surface Laptop",
    "similarity": 0.72
  }
]
```

### 5. Trending Products

```bash
GET /api/search/trending?category=electronics&limit=10

Response: 200 OK
[
  {
    "id": "LAPTOP-001",
    "name": "MacBook Pro",
    "viewCount": 5234,
    "purchaseCount": 234
  }
]
```

### 6. Reindex All Products (Admin)

```bash
POST /api/search/admin/reindex

Response: 202 Accepted
{
  "message": "Reindexing started",
  "totalProducts": 1000,
  "estimatedTime": "30 seconds"
}
```

---

## Search Features

### 1. Full-Text Search

**Query:** "macbook pro 16"

**Elasticsearch Query:**
```json
{
  "query": {
    "multi_match": {
      "query": "macbook pro 16",
      "fields": ["name^3", "description^2", "tags"],
      "type": "best_fields",
      "fuzziness": "AUTO"
    }
  }
}
```

**Field Boosting:**
- `name^3` - Name is 3x more important
- `description^2` - Description is 2x more important
- `tags` - Regular weight

### 2. Fuzzy Search (Typo Tolerance)

**Query:** "mackbook" (typo)

**Result:** Finds "macbook" ✅

**How:** Levenshtein distance (edit distance ≤ 2)

### 3. Filters

**Multiple Filters:**
```bash
GET /api/search?q=laptop
  &category=electronics
  &brand=Apple
  &minPrice=1000
  &maxPrice=3000
  &minRating=4.5
  &inStock=true
```

**Elasticsearch Query:**
```json
{
  "query": {
    "bool": {
      "must": [
        {"multi_match": {"query": "laptop", "fields": ["name", "description"]}}
      ],
      "filter": [
        {"term": {"category": "electronics"}},
        {"term": {"brand": "Apple"}},
        {"range": {"price": {"gte": 1000, "lte": 3000}}},
        {"range": {"rating": {"gte": 4.5}}},
        {"term": {"inStock": true}}
      ]
    }
  }
}
```

### 4. Faceted Search

**Response includes aggregations:**
```json
{
  "facets": {
    "categories": {
      "electronics": 120,
      "computers": 85,
      "accessories": 45
    },
    "brands": {
      "Apple": 35,
      "Dell": 28,
      "HP": 22
    },
    "priceRanges": {
      "0-500": 45,
      "500-1000": 78,
      "1000-2000": 92,
      "2000+": 35
    }
  }
}
```

**Use Case:** Dynamic filters on UI

### 5. Autocomplete

**Implementation:**
```json
{
  "suggest": {
    "product-suggest": {
      "prefix": "lap",
      "completion": {
        "field": "name.suggest",
        "size": 5,
        "skip_duplicates": true
      }
    }
  }
}
```

**User Types:** "lap"  
**Suggestions:** laptop, laptop bag, laptop stand

### 6. Sorting

**Relevance (Default):**
```
Based on score (TF-IDF + BM25)
```

**Price Ascending:**
```json
{"sort": [{"price": "asc"}]}
```

**Rating Descending:**
```json
{"sort": [{"rating": "desc"}]}
```

**Newest First:**
```json
{"sort": [{"createdAt": "desc"}]}
```

---

## Elasticsearch Index Mapping

### products Index

```json
{
  "mappings": {
    "properties": {
      "id": {"type": "keyword"},
      "name": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": {"type": "keyword"},
          "suggest": {"type": "completion"}
        }
      },
      "description": {
        "type": "text",
        "analyzer": "standard"
      },
      "category": {"type": "keyword"},
      "brand": {"type": "keyword"},
      "price": {"type": "double"},
      "rating": {"type": "float"},
      "inStock": {"type": "boolean"},
      "tags": {"type": "keyword"},
      "imageUrl": {"type": "keyword"},
      "createdAt": {"type": "date"},
      "updatedAt": {"type": "date"}
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "product_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "stop", "snowball"]
        }
      }
    }
  }
}
```

---

## Configuration

### Environment Variables

```bash
# Elasticsearch
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Catalog Service
CATALOG_SERVICE_URL=http://localhost:8080

# Search Settings
SEARCH_INDEX_NAME=products
SEARCH_SYNC_ON_STARTUP=true
SEARCH_BATCH_SIZE=100

# Server
SERVER_PORT=8088
```

---

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
# Uses Testcontainers (Elasticsearch)
mvn test -Dtest=SearchIntegrationTest
```

### Manual Testing

#### 1. Index Sample Data

```bash
# Start Catalog Service (port 8080)
# Search Service will sync automatically on startup

# Or manually trigger reindex
curl -X POST http://localhost:8088/api/search/admin/reindex
```

#### 2. Search Products

```bash
# Basic search
curl "http://localhost:8088/api/search?q=laptop" | jq

# With filters
curl "http://localhost:8088/api/search?q=laptop&category=electronics&minPrice=1000&maxPrice=2000" | jq

# Sort by price
curl "http://localhost:8088/api/search?q=laptop&sort=price_asc" | jq
```

#### 3. Autocomplete

```bash
curl "http://localhost:8088/api/search/suggest?q=lap" | jq
```

#### 4. Check Elasticsearch Index

```bash
# Index stats
curl http://localhost:9200/products/_stats | jq

# Sample documents
curl http://localhost:9200/products/_search?size=5 | jq

# Index mapping
curl http://localhost:9200/products/_mapping | jq
```

---

## Troubleshooting

### Elasticsearch Connection Failed

```bash
# Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Start if not running
docker start elasticsearch

# Check logs
docker logs elasticsearch
```

### No Search Results

```bash
# Check index exists
curl http://localhost:9200/_cat/indices

# Check document count
curl http://localhost:9200/products/_count

# If empty, trigger reindex
curl -X POST http://localhost:8088/api/search/admin/reindex
```

### Slow Queries

```bash
# Enable slow log
curl -X PUT http://localhost:9200/products/_settings -H 'Content-Type: application/json' -d '{
  "index.search.slowlog.threshold.query.warn": "1s",
  "index.search.slowlog.threshold.query.info": "500ms"
}'

# Check slow queries
docker logs elasticsearch | grep "slowlog"
```

---

## Week 9 Learning Summary

### 1. Elasticsearch Basics

**What:** Search engine built on Apache Lucene  
**Why:** Fast full-text search (milliseconds for millions of docs)  
**How:** Inverted index + scoring algorithms

### 2. Inverted Index

**Traditional Database:**
```
Doc 1: "MacBook Pro"
Doc 2: "Dell Laptop"
Doc 3: "MacBook Air"

Query: "MacBook" → Scan all docs ❌
```

**Inverted Index:**
```
Term      → Documents
"macbook" → [Doc 1, Doc 3]
"pro"     → [Doc 1]
"dell"    → [Doc 2]
"laptop"  → [Doc 2]
"air"     → [Doc 3]

Query: "MacBook" → Instant lookup: [Doc 1, Doc 3] ✅
```

### 3. Relevance Scoring (BM25)

**TF-IDF:** Term Frequency * Inverse Document Frequency

```
Query: "MacBook Pro"

Doc 1: "MacBook Pro 16-inch" → High score (both terms)
Doc 2: "MacBook Air" → Medium score (one term)
Doc 3: "Dell Laptop" → Low score (no terms)
```

### 4. Event-Driven Indexing

**Synchronous (Bad):**
```java
// Catalog Service
product.save();
searchService.indexProduct(product);  // ❌ Tight coupling
```

**Asynchronous (Good):**
```java
// Catalog Service
product.save();
kafkaProducer.send("ProductCreated", product);  // ✅ Decoupled

// Search Service (separate)
@KafkaListener
void onProductCreated(Product product) {
    elasticsearchRepo.save(product);
}
```

### 5. Autocomplete Implementation

**Completion Suggester:**
```json
{
  "name": {
    "type": "text",
    "fields": {
      "suggest": {
        "type": "completion"
      }
    }
  }
}
```

**Optimized data structure:**
- FST (Finite State Transducer)
- Prefix tree lookup: O(k) where k = prefix length
- Fast: <10ms for millions of terms

---

## Project Structure

```
search-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/search/
│   │   │   ├── SearchServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── SearchController.java
│   │   │   │   └── AdminController.java
│   │   │   ├── service/
│   │   │   │   ├── SearchService.java
│   │   │   │   ├── IndexService.java
│   │   │   │   └── SyncService.java
│   │   │   ├── repository/
│   │   │   │   └── ProductSearchRepository.java
│   │   │   ├── entity/
│   │   │   │   └── ProductDocument.java
│   │   │   ├── dto/
│   │   │   │   ├── SearchRequest.java
│   │   │   │   ├── SearchResponse.java
│   │   │   │   └── SuggestionResponse.java
│   │   │   ├── listener/
│   │   │   │   └── CatalogEventListener.java
│   │   │   └── config/
│   │   │       ├── ElasticsearchConfig.java
│   │   │       └── KafkaConfig.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Next Steps (Week 10+)

- [ ] Synonyms (laptop = notebook)
- [ ] Spell correction
- [ ] Voice search
- [ ] Image search
- [ ] Personalized results
- [ ] A/B testing search algorithms
- [ ] Analytics (popular searches)
- [ ] Cache frequent queries (Redis)
- [ ] Multi-language support
- [ ] Geo-search (nearby stores)

---

## API Documentation

- **Swagger UI**: http://localhost:8088/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8088/api-docs

---

## Build Commands

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -Dmaven.test.skip=true

# Run tests
mvn test

# Run locally
mvn spring-boot:run

# Build Docker image
docker build -t search-service:latest .
```

---

## Dependencies

- Spring Boot 3.1.6
- Spring Data Elasticsearch
- Spring Kafka
- Spring WebFlux (WebClient)
- Elasticsearch 8.11.0
- Testcontainers

---

## Service Comparison

| Feature | Catalog | Search |
|---------|---------|--------|
| **Port** | 8080 | 8088 |
| **Database** | PostgreSQL | Elasticsearch |
| **Purpose** | Store products | Search products |
| **Queries** | CRUD | Full-text search |
| **Performance** | Good for exact match | Optimized for text search |
| **Features** | ACID transactions | Fuzzy, autocomplete, facets |

---

**Last Updated:** December 11, 2025  
**Version:** 0.0.1-SNAPSHOT  
**Status:** Week 9 (Search) Complete ✅

