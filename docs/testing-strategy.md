# Testing Strategy — E-Commerce Microservices Platform

**Generated:** 2025-11-27  
**Status:** Active guideline for all services

---

## Executive Summary

**YES, you should write unit tests EVEN when using Testcontainers integration tests.**

Integration tests and unit tests serve **different purposes** and complement each other in a comprehensive testing strategy. This document explains why both are necessary and provides guidelines for implementing each type.

---

## Testing Pyramid

```
                /\
               /  \
              /E2E \         <- Few (5-10% of tests)
             /______\
            /        \
           / Integr.  \      <- Some (20-30% of tests)
          /____________\
         /              \
        /   Unit Tests   \   <- Many (60-75% of tests)
       /__________________\
```

---

## Why BOTH Unit Tests AND Integration Tests?

### Integration Tests (Testcontainers)
**What they test:**
- Full flow through all layers (Controller → Service → Repository → Database)
- Database constraints and transactions
- HTTP request/response serialization
- Spring configuration and dependency injection
- Real database behavior (Postgres-specific features)
- API contract compliance

**Limitations:**
- **Slow** (5-30 seconds per test due to container startup)
- **Complex failures** (hard to pinpoint which component failed)
- **Environment-dependent** (Docker required, resource-intensive)
- **Cannot test edge cases easily** (complex mock scenarios)
- **Expensive in CI/CD** (longer build times, more resources)

### Unit Tests
**What they test:**
- **Business logic in isolation**
- **Edge cases and error handling**
- **Validation logic**
- **Transformations and calculations**
- **Complex conditional flows**
- **Exception scenarios**

**Benefits:**
- **Fast** (milliseconds per test)
- **Precise failure identification** (exact method/line that failed)
- **No external dependencies** (no Docker, no DB)
- **Easy to test edge cases** (mock any scenario)
- **Cheap in CI/CD** (fast feedback loop)
- **Better code design** (forces good separation of concerns)

---

## Testing Strategy by Layer

### 1. Service Layer (Business Logic)
**Unit Tests: YES (Primary Testing Method)**

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private ItemService itemService;
    
    @Test
    void shouldCreateItem() {
        // Test business logic without database
        Item item = new Item("Laptop", 999.99);
        when(itemRepository.save(any())).thenReturn(item);
        
        Item result = itemService.createItem(item);
        
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(itemRepository).save(item);
    }
    
    @Test
    void shouldThrowExceptionWhenItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, 
            () -> itemService.updateItem(1L, new Item()));
    }
    
    @Test
    void shouldValidatePriceIsPositive() {
        // Test business rule: price must be positive
        Item item = new Item("Laptop", -100.0);
        
        assertThrows(IllegalArgumentException.class,
            () -> itemService.createItem(item));
    }
}
```

**Why:** Fast execution, tests business logic without database overhead

---

### 2. Repository Layer
**Unit Tests: NO (Use Integration Tests)**

Repository logic is typically thin (Spring Data JPA) and needs real database testing.

**Integration Tests: YES**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ItemRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private ItemRepository repository;
    
    @Test
    void shouldFindItemsByCategory() {
        repository.save(new Item("Laptop", "Electronics", 999.99));
        
        List<Item> items = repository.findByCategory("Electronics");
        
        assertThat(items).hasSize(1);
    }
}
```

---

### 3. Controller Layer
**Unit Tests: YES (for validation and mapping)**

```java
@WebMvcTest(CatalogController.class)
class CatalogControllerUnitTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ItemService itemService;
    
    @Test
    void shouldReturn400WhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/catalog/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldMapItemToDTO() throws Exception {
        Item item = new Item(1L, "Laptop", 999.99);
        when(itemService.getItemById(1L)).thenReturn(Optional.of(item));
        
        mockMvc.perform(get("/catalog/items/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Laptop"))
            .andExpect(jsonPath("$.price").value(999.99));
    }
}
```

**Integration Tests: YES (for full flow)**

Already implemented in `CatalogControllerIntegrationTest` - tests entire stack.

---

## Recommended Test Distribution

### For Each Service:

| Test Type | Count | Purpose | Speed |
|-----------|-------|---------|-------|
| **Unit Tests (Service)** | 30-50 | Business logic, edge cases | <1s total |
| **Unit Tests (Controller)** | 10-15 | Validation, mapping | <1s total |
| **Integration Tests (API)** | 10-20 | Full flow, happy paths | 30-60s total |
| **Integration Tests (Repository)** | 5-10 | Complex queries | 10-20s total |

**Total test execution time: ~1-2 minutes per service**

---

## Best Practices

### ✅ DO:
1. **Write unit tests for all service methods** with business logic
2. **Write unit tests for complex controllers** with validation rules
3. **Write integration tests for critical flows** (checkout, payment)
4. **Use Testcontainers for integration tests** (real DB behavior)
5. **Mock external service calls** in unit tests
6. **Test edge cases in unit tests** (null values, boundary conditions)
7. **Test database constraints in integration tests**

### ❌ DON'T:
1. **Don't only write integration tests** (slow feedback, hard to debug)
2. **Don't unit test simple getters/setters** (waste of time)
3. **Don't unit test Spring framework code** (already tested by Spring)
4. **Don't use real database in unit tests** (defeats the purpose)
5. **Don't skip unit tests because you have integration tests**

---

## Specific Scenarios

### When to Write Unit Tests:

✅ **Service with price calculation logic**
```java
public double calculateDiscountedPrice(Item item, Discount discount) {
    // Complex calculation - UNIT TEST THIS
}
```

✅ **Validation logic**
```java
public void validateOrder(Order order) {
    if (order.getItems().isEmpty()) throw new ValidationException();
    // Business rules - UNIT TEST THIS
}
```

✅ **State machine transitions**
```java
public OrderState transition(OrderState current, OrderEvent event) {
    // State logic - UNIT TEST THIS
}
```

✅ **Data transformations**
```java
public ItemDTO toDTO(Item item) {
    // Complex mapping - UNIT TEST IF COMPLEX
}
```

### When Integration Tests Are Sufficient:

✅ **Simple CRUD repositories** (Spring Data JPA)
```java
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByCategory(String category);
}
```

✅ **Pass-through controllers** (no logic)
```java
@GetMapping("/{id}")
public ItemDTO getItem(@PathVariable Long id) {
    return itemService.getItemById(id);
}
```

---

## Example: Comprehensive Test Suite

### Service Class (with business logic)
```java
@Service
public class PricingService {
    
    @Autowired
    private PricingRepository repository;
    
    public double calculateFinalPrice(Long itemId, String customerTier) {
        Item item = repository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));
            
        double basePrice = item.getPrice();
        double discount = getDiscountForTier(customerTier);
        
        return basePrice * (1 - discount);
    }
    
    private double getDiscountForTier(String tier) {
        return switch (tier) {
            case "GOLD" -> 0.20;
            case "SILVER" -> 0.10;
            case "BRONZE" -> 0.05;
            default -> 0.0;
        };
    }
}
```

### Unit Test (Fast - Tests Logic)
```java
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {
    
    @Mock
    private PricingRepository repository;
    
    @InjectMocks
    private PricingService pricingService;
    
    @Test
    void shouldApplyGoldDiscount() {
        Item item = new Item(1L, "Laptop", 1000.0);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        
        double finalPrice = pricingService.calculateFinalPrice(1L, "GOLD");
        
        assertThat(finalPrice).isEqualTo(800.0);
    }
    
    @Test
    void shouldApplyNoDiscountForUnknownTier() {
        Item item = new Item(1L, "Laptop", 1000.0);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        
        double finalPrice = pricingService.calculateFinalPrice(1L, "UNKNOWN");
        
        assertThat(finalPrice).isEqualTo(1000.0);
    }
    
    @Test
    void shouldThrowExceptionWhenItemNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(ItemNotFoundException.class,
            () -> pricingService.calculateFinalPrice(999L, "GOLD"));
    }
}
```

### Integration Test (Slow - Tests Full Stack)
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PricingServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private PricingService pricingService;
    
    @Autowired
    private PricingRepository repository;
    
    @Test
    void shouldCalculatePriceEndToEnd() {
        // Test with real database
        Item item = repository.save(new Item("Laptop", 1000.0));
        
        double finalPrice = pricingService.calculateFinalPrice(item.getId(), "GOLD");
        
        assertThat(finalPrice).isEqualTo(800.0);
    }
}
```

**Notice:**
- Unit test: 3 test methods, runs in ~50ms
- Integration test: 1 test method, runs in ~5s (container startup)

**Both are needed!** Unit tests catch logic bugs fast, integration test validates full stack.

---

## CI/CD Integration

### GitHub Actions Workflow
```yaml
name: Test Service

on: [pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Unit Tests (Fast)
        run: mvn test -Dtest=*Test
        
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Integration Tests (Slow)
        run: mvn verify -Dtest=*IntegrationTest
```

**Benefits:**
- Fast feedback from unit tests (30s)
- Comprehensive validation from integration tests (2-5min)
- Can fail fast on unit tests before running expensive integration tests

---

## Summary & Decision

### ✅ **YES, Write Unit Tests Even With Testcontainers**

**Reasons:**
1. **Speed:** Unit tests provide instant feedback (CI fails in 30s vs 5min)
2. **Precision:** Pinpoints exact failing method
3. **Coverage:** Tests edge cases that are hard to reproduce in integration tests
4. **Cost:** Cheaper CI/CD execution
5. **Design:** Forces better code structure and separation of concerns
6. **Debugging:** Easier to identify root cause

### Recommended Approach:
1. **Start with unit tests** for all service layer logic
2. **Add integration tests** for critical flows and repository layer
3. **Use Testcontainers** for integration tests to ensure real DB behavior
4. **Maintain 60/30/10 split** (60% unit, 30% integration, 10% E2E)

### Action Items:
- [ ] Add unit tests for ItemService methods
- [ ] Add unit tests for complex controller validation
- [ ] Keep existing integration tests with Testcontainers
- [ ] Document test strategy in each service's README
- [ ] Set up CI to run unit tests first, then integration tests

---

## Next Steps

See `docs/testing-examples/` for complete examples:
- `unit-test-examples/`
- `integration-test-examples/`
- `contract-test-examples/`

---

**References:**
- Martin Fowler's Testing Pyramid: https://martinfowler.com/articles/practical-test-pyramid.html
- Spring Boot Testing Guide: https://spring.io/guides/gs/testing-web/
- Testcontainers Documentation: https://www.testcontainers.org/

