# Pricing Service Performance Tests

This directory contains k6 performance test scripts for the Pricing Service.

## 📦 Prerequisites

1. **Install k6**:
   ```bash
   # macOS
   brew install k6
   
   # Linux (Debian/Ubuntu)
   sudo gpg -k
   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update
   sudo apt-get install k6
   
   # Windows (with Chocolatey)
   choco install k6
   
   # Or download binary from: https://k6.io/docs/get-started/installation/
   ```

2. **Start Pricing Service**:
   ```bash
   # From project root
   cd infra && docker compose up -d pricing-postgres redis
   cd ../services/pricing-service && mvn spring-boot:run
   ```

## 🧪 Test Scripts

### 1. Smoke Test (`smoke-test.k6.js`)
Quick verification that the service is working correctly with minimal load.

- **Duration**: 30 seconds
- **Virtual Users**: 1
- **Purpose**: Verify basic functionality

**Run**:
```bash
k6 run smoke-test.k6.js
```

**Expected Output**:
- All checks passing
- Response times < 1s
- Error rate < 5%

### 2. Load Test (`pricing-load-test.k6.js`)
Comprehensive load test simulating realistic traffic patterns.

- **Duration**: 6.5 minutes
- **Max Virtual Users**: 100
- **Load Pattern**: Gradual ramp-up and ramp-down
- **Test Scenarios**:
  - 70% price lookups (tests caching)
  - 20% list all rules
  - 5% update operations
  - 30% bulk pricing lookups

**Run**:
```bash
k6 run pricing-load-test.k6.js
```

**Custom Configuration**:
```bash
# Override base URL
k6 run --env BASE_URL=http://localhost:8082 pricing-load-test.k6.js

# Different load profile
k6 run --vus 50 --duration 2m pricing-load-test.k6.js
```

## 📊 Metrics & Thresholds

### Key Performance Indicators (KPIs)

| Metric | Target (p95) | Target (p99) |
|--------|-------------|-------------|
| HTTP Request Duration | < 500ms | < 1000ms |
| Price Lookup Duration | < 300ms | < 800ms |
| Error Rate | < 1% | < 1% |
| Cache Hit Rate | > 80% | - |

### Custom Metrics

The load test includes custom metrics:

- **`price_lookup_duration`**: Time taken for price lookup operations
- **`cache_hits`**: Rate of cache hits vs database queries
- **`api_calls_total`**: Total number of API calls made
- **`errors`**: Custom error tracking

## 🎯 Performance Benchmarks

### Expected Results (Local Development)

Based on the service running on localhost with Redis and PostgreSQL:

**Smoke Test**:
- ✅ All requests succeed
- ✅ Average response time: 50-100ms
- ✅ p95 response time: < 200ms

**Load Test** (100 concurrent users):
- ✅ Total requests: ~30,000-40,000
- ✅ Requests per second: ~100-150 RPS
- ✅ Average response time: 100-200ms
- ✅ p95 response time: < 500ms
- ✅ Error rate: < 0.1%
- ✅ Cache hit rate: 80-90%

### Caching Behavior Verification

The tests verify Redis caching effectiveness:

1. **First request** for an item → `source: "DATABASE"` (cache miss)
2. **Subsequent requests** → `source: "CACHE"` (cache hit)
3. **After update/delete** → Cache invalidated, next request from database

Expected cache hit rate: **80-90%** in steady-state load.

## 📈 Running Performance Benchmarks

### Full Benchmark Suite
```bash
#!/bin/bash
# Run complete performance benchmark

echo "=== Starting Performance Benchmark ==="

# 1. Smoke test
echo "Running smoke test..."
k6 run smoke-test.k6.js

# 2. Load test
echo "Running load test..."
k6 run pricing-load-test.k6.js

echo "=== Benchmark Complete ==="
```

### Continuous Load Testing
For longer-running stability tests:
```bash
# Run for 30 minutes with 50 users
k6 run --vus 50 --duration 30m pricing-load-test.k6.js
```

## 🔍 Analyzing Results

### Reading k6 Output

```
✓ price lookup status is 200         100.00% ✓ 25432 ✗ 0
✓ price lookup has itemId            100.00% ✓ 25432 ✗ 0
✓ cache_hits                          87.23%  ✓ 22183 ✗ 3249

http_req_duration..............: avg=145.32ms p95=387.21ms p99=654.89ms
price_lookup_duration..........: avg=89.45ms  p95=245.12ms p99=421.34ms
```

**What to look for**:
- ✅ Check marks indicate passing thresholds
- ✗ Crosses indicate failing thresholds
- Cache hit rate should be > 80%
- p95 latency should be < 500ms
- Error rate should be < 1%

### Common Issues

**High Latency (> 1s)**:
- Check database connection pool size
- Verify Redis is running and accessible
- Check for slow queries in logs

**Low Cache Hit Rate (< 50%)**:
- Verify Redis is running: `docker ps | grep redis`
- Check cache TTL configuration
- Ensure cache invalidation isn't too aggressive

**High Error Rate (> 1%)**:
- Check service logs: `docker logs pricing-service`
- Verify database and Redis connectivity
- Check for resource exhaustion (CPU, memory)

## 🚀 CI/CD Integration

### GitHub Actions Example
```yaml
name: Performance Tests

on:
  push:
    branches: [ main ]
    paths:
      - 'services/pricing-service/**'

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Start infrastructure
        run: |
          cd infra
          docker compose up -d pricing-postgres redis
      
      - name: Build and start service
        run: |
          cd services/pricing-service
          mvn clean package -DskipTests
          java -jar target/*.jar &
          sleep 30
      
      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6
      
      - name: Run smoke test
        run: |
          cd services/pricing-service/performance-tests
          k6 run smoke-test.k6.js
      
      - name: Run load test
        run: |
          cd services/pricing-service/performance-tests
          k6 run pricing-load-test.k6.js
```

## 📝 Best Practices

1. **Baseline Performance**: Run tests regularly to establish baseline metrics
2. **Pre-Release Testing**: Always run smoke tests before deploying
3. **Load Testing**: Run comprehensive load tests weekly or before major releases
4. **Monitor Trends**: Track performance metrics over time
5. **Test Realistic Scenarios**: Adjust test data and patterns to match production traffic

## 🔧 Troubleshooting

### k6 Installation Issues
```bash
# Verify k6 installation
k6 version

# If not found, download binary directly
wget https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz
tar -xzf k6-v0.47.0-linux-amd64.tar.gz
sudo mv k6-v0.47.0-linux-amd64/k6 /usr/local/bin/
```

### Service Not Responding
```bash
# Check service health
curl http://localhost:8083/actuator/health

# Check logs
mvn spring-boot:run

# Verify infrastructure
cd infra && docker compose ps
```

### Test Failures
```bash
# Run with verbose output
k6 run --verbose smoke-test.k6.js

# Enable HTTP debug logging
k6 run --http-debug smoke-test.k6.js
```

## 📚 Additional Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Test Types](https://k6.io/docs/test-types/introduction/)
- [k6 Thresholds Guide](https://k6.io/docs/using-k6/thresholds/)
- [Performance Testing Best Practices](https://k6.io/docs/testing-guides/api-load-testing/)

## 🤝 Contributing

When adding new performance tests:
1. Follow existing naming conventions
2. Include setup and teardown functions
3. Add appropriate thresholds
4. Document expected results
5. Update this README with test description
