# E2E Testing Quick Reference Card

## 📊 The 60/30/10 Rule

```
Unit Tests (60%)        → Fast, cheap, many
Integration Tests (30%) → Medium speed/cost
E2E Tests (10%)         → Slow, expensive, few
```

---

## 🎯 What to Test Where

### Unit Test ✅
- Business logic calculations
- Validation rules
- Edge cases and error handling
- Complex algorithms
- Data transformations

### Integration Test ✅
- Database interactions
- API endpoints
- Spring configuration
- Repository queries
- Single service flows

### E2E Test ✅
- Complete user journeys
- Cross-service workflows
- Event-driven flows
- Authentication end-to-end
- Critical business processes

---

## 🚦 E2E Test Checklist

### When to Write E2E Test
- ✅ Critical business flow (purchase, checkout)
- ✅ Multiple services involved (3+)
- ✅ Events are published/consumed
- ✅ High business value
- ✅ Cannot be tested at lower levels

### When NOT to Write E2E Test
- ❌ Can be tested with unit test
- ❌ Can be tested with integration test
- ❌ Edge case or validation
- ❌ Single service functionality
- ❌ Low business impact

---

## 💡 E2E Test Examples for E-Commerce

### ✅ DO Write E2E Tests For:

1. **Complete Purchase Flow**
   ```
   Browse → Add to Cart → Checkout → Payment → Confirmation
   Services: Catalog, Cart, Inventory, Pricing, Order, Payment
   ```

2. **Order Cancellation**
   ```
   Create Order → Cancel → Refund → Restore Inventory
   Services: Order, Payment, Inventory
   Events: OrderCancelled, PaymentRefunded, InventoryRestored
   ```

3. **Out of Stock Scenario**
   ```
   Add to Cart → Checkout → Inventory Check Fails → Order Rejected
   Services: Cart, Inventory, Order
   ```

4. **User Registration & First Purchase**
   ```
   Register → Login → Browse → Add to Cart → Checkout
   Services: Auth, Catalog, Cart, Order, Payment
   ```

5. **Inventory Update Flow**
   ```
   Order Placed → Event Published → Inventory Decremented
   Services: Order, Inventory
   Events: OrderCreated, InventoryReserved
   ```

### ❌ DON'T Write E2E Tests For:

- ❌ Price calculation with discount (unit test)
- ❌ Invalid email validation (unit test)
- ❌ Database query performance (integration test)
- ❌ Single API endpoint (integration test)
- ❌ Error message format (unit test)

---

## 🛠️ How to Run

### Local (Docker Compose)
```bash
# Start services
docker-compose -f docker-compose.e2e.yml up -d

# Run tests
mvn test -Dtest=*E2ETest

# Stop services
docker-compose -f docker-compose.e2e.yml down
```

### CI/CD Pipeline
```yaml
# Run after unit & integration tests pass
e2e-tests:
  needs: [unit-tests, integration-tests]
  runs-on: ubuntu-latest
  steps:
    - name: Start Services
      run: docker-compose -f docker-compose.e2e.yml up -d
    - name: Wait for Health
      run: ./wait-for-services.sh
    - name: Run E2E Tests
      run: mvn test -Dtest=*E2ETest
    - name: Cleanup
      run: docker-compose -f docker-compose.e2e.yml down
```

---

## ⏱️ Performance Targets

| Test Type | Target Time | Action if Slower |
|-----------|-------------|------------------|
| Unit | < 100ms | Optimize mocks |
| Integration | < 5s | Check Testcontainers config |
| E2E | < 60s | Reduce test scope or parallelize |

---

## 🐛 Debugging Failed E2E Tests

### Step 1: Identify Which Service Failed
```bash
# Check service logs
docker-compose -f docker-compose.e2e.yml logs catalog-service
docker-compose -f docker-compose.e2e.yml logs order-service
```

### Step 2: Check Service Health
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Step 3: Verify Events
```bash
# Check Kafka messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

### Step 4: Reproduce with Integration Test
- Isolate the failing service
- Write an integration test for that service
- Fix the issue at the integration level
- Re-run E2E test

---

## 📐 Test Pyramid Metrics

Track these metrics to maintain healthy pyramid:

```bash
# Count tests by type
Unit:        grep -r "@Test" */src/test/java/**/*Test.java | wc -l
Integration: grep -r "@Test" */src/test/java/**/*IntegrationTest.java | wc -l
E2E:         grep -r "@Test" */src/test/java/**/*E2ETest.java | wc -l

# Calculate ratio
Total = Unit + Integration + E2E
Unit % = (Unit / Total) * 100
Integration % = (Integration / Total) * 100
E2E % = (E2E / Total) * 100

# Target: 60% / 30% / 10%
```

---

## 🎓 Quick Decision Tree

```
Need to write a test?
│
├─ Does it test ONE method in isolation?
│  └─ YES → Unit Test ✅
│
├─ Does it test ONE service with database?
│  └─ YES → Integration Test ✅
│
└─ Does it test MULTIPLE services together?
   │
   ├─ Is it a critical user journey?
   │  └─ YES → E2E Test ✅
   │
   └─ Is it an edge case or validation?
      └─ YES → Unit/Integration Test instead ⚠️
```

---

## 📚 Further Reading

- **Full Guide**: `docs/testing-strategy.md`
- **Setup Instructions**: `UNIT_TESTING_SETUP.md`
- **Unit Test Examples**: `services/catalog-service/src/test/java/**/*Test.java`
- **Integration Test Examples**: `services/catalog-service/src/test/java/**/*IntegrationTest.java`

---

## 🚀 Pro Tips

1. **Start Small**: Begin with 1-2 critical E2E tests
2. **Fail Fast**: Run unit tests first in CI/CD
3. **Keep E2E Tests Stable**: Don't test edge cases in E2E
4. **Use Test Data Builders**: Make E2E tests readable
5. **Monitor Test Duration**: Alert if E2E tests take > 60s
6. **Parallelize**: Run E2E tests in parallel when possible
7. **Document Failures**: Add troubleshooting guides
8. **Version Test Data**: Keep test data in version control

---

**Remember**: The best test is the one at the lowest level that gives you confidence! 🎯

