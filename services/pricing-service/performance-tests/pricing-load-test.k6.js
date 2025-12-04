import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const priceLookupDuration = new Trend('price_lookup_duration');
const cacheHitRate = new Rate('cache_hits');
const apiCallsTotal = new Counter('api_calls_total');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '2m', target: 100 },   // Stay at 100 users
    { duration: '1m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95% of requests should be below 500ms, 99% below 1s
    'http_req_failed': ['rate<0.01'],  // Error rate should be less than 1%
    'errors': ['rate<0.01'],            // Custom error rate should be less than 1%
    'price_lookup_duration': ['p(95)<300', 'p(99)<800'], // Price lookups should be fast
  },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const API_PREFIX = '/api/v1/pricing';

// Test data
const testItems = [
  { itemId: 'LAPTOP-001', basePrice: 1200.00, discountPercent: 20.00 },
  { itemId: 'PHONE-001', basePrice: 800.00, discountPercent: 15.00 },
  { itemId: 'TABLET-001', basePrice: 600.00, discountPercent: 10.00 },
  { itemId: 'MONITOR-001', basePrice: 400.00, discountPercent: 5.00 },
  { itemId: 'KEYBOARD-001', basePrice: 100.00, discountPercent: 0.00 },
];

// Setup: Create pricing rules for test items
export function setup() {
  console.log('Setting up test data...');
  const ruleIds = [];

  testItems.forEach(item => {
    const payload = JSON.stringify({
      itemId: item.itemId,
      basePrice: item.basePrice,
      discountPercent: item.discountPercent,
      currency: 'USD',
      ruleType: 'PROMOTIONAL',
      status: 'ACTIVE'
    });

    const res = http.post(`${BASE_URL}${API_PREFIX}/rules`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 201) {
      const rule = JSON.parse(res.body);
      ruleIds.push(rule.id);
      console.log(`Created pricing rule for ${item.itemId} with ID: ${rule.id}`);
    } else {
      console.error(`Failed to create pricing rule for ${item.itemId}: ${res.status}`);
    }
  });

  return { ruleIds };
}

// Main test scenario
export default function (data) {
  // Test 1: Get price for item (tests caching behavior)
  group('Price Lookup', () => {
    const itemId = testItems[Math.floor(Math.random() * testItems.length)].itemId;
    const startTime = new Date();
    
    const res = http.get(`${BASE_URL}${API_PREFIX}/price/${itemId}`);
    
    const duration = new Date() - startTime;
    priceLookupDuration.add(duration);
    apiCallsTotal.add(1);

    const success = check(res, {
      'price lookup status is 200': (r) => r.status === 200,
      'price lookup has itemId': (r) => JSON.parse(r.body).itemId === itemId,
      'price lookup has price': (r) => JSON.parse(r.body).price !== undefined,
      'price lookup has currency': (r) => JSON.parse(r.body).currency === 'USD',
    });

    if (!success) {
      errorRate.add(1);
      console.error(`Price lookup failed for ${itemId}: ${res.status} - ${res.body}`);
    } else {
      errorRate.add(0);
      const body = JSON.parse(res.body);
      // Track cache hits vs database queries
      if (body.source === 'CACHE') {
        cacheHitRate.add(1);
      } else {
        cacheHitRate.add(0);
      }
    }
  });

  sleep(0.1); // Small delay between iterations

  // Test 2: Get price with quantity (bulk pricing)
  if (Math.random() < 0.3) { // 30% of requests test bulk pricing
    group('Bulk Price Lookup', () => {
      const itemId = testItems[Math.floor(Math.random() * testItems.length)].itemId;
      const quantity = Math.floor(Math.random() * 20) + 1;
      
      const res = http.get(`${BASE_URL}${API_PREFIX}/price/${itemId}/quantity/${quantity}`);
      apiCallsTotal.add(1);

      const success = check(res, {
        'bulk price lookup status is 200': (r) => r.status === 200,
        'bulk price has itemId': (r) => JSON.parse(r.body).itemId === itemId,
        'bulk price has price': (r) => JSON.parse(r.body).price !== undefined,
      });

      if (!success) {
        errorRate.add(1);
      } else {
        errorRate.add(0);
      }
    });
    sleep(0.1);
  }

  // Test 3: Get all pricing rules (read-heavy operation)
  if (Math.random() < 0.2) { // 20% of requests fetch all rules
    group('List All Rules', () => {
      const res = http.get(`${BASE_URL}${API_PREFIX}/rules`);
      apiCallsTotal.add(1);

      const success = check(res, {
        'list rules status is 200': (r) => r.status === 200,
        'list rules returns array': (r) => Array.isArray(JSON.parse(r.body)),
      });

      if (!success) {
        errorRate.add(1);
      } else {
        errorRate.add(0);
      }
    });
    sleep(0.1);
  }

  // Test 4: Update pricing rule (write operation - low frequency)
  if (Math.random() < 0.05 && data.ruleIds && data.ruleIds.length > 0) { // 5% of requests update a rule
    group('Update Rule', () => {
      const ruleId = data.ruleIds[Math.floor(Math.random() * data.ruleIds.length)];
      const item = testItems[Math.floor(Math.random() * testItems.length)];
      
      const payload = JSON.stringify({
        itemId: item.itemId,
        basePrice: item.basePrice + Math.random() * 100,
        discountPercent: Math.floor(Math.random() * 30),
        currency: 'USD',
        ruleType: 'SEASONAL',
        status: 'ACTIVE'
      });

      const res = http.put(`${BASE_URL}${API_PREFIX}/rules/${ruleId}`, payload, {
        headers: { 'Content-Type': 'application/json' },
      });
      apiCallsTotal.add(1);

      const success = check(res, {
        'update rule status is 200': (r) => r.status === 200,
        'update rule has finalPrice': (r) => JSON.parse(r.body).finalPrice !== undefined,
      });

      if (!success) {
        errorRate.add(1);
      } else {
        errorRate.add(0);
      }
    });
    sleep(0.2);
  }
}

// Teardown: Clean up test data
export function teardown(data) {
  console.log('Cleaning up test data...');
  
  if (data.ruleIds) {
    data.ruleIds.forEach(ruleId => {
      const res = http.del(`${BASE_URL}${API_PREFIX}/rules/${ruleId}`);
      if (res.status === 204) {
        console.log(`Deleted pricing rule with ID: ${ruleId}`);
      } else {
        console.error(`Failed to delete pricing rule ${ruleId}: ${res.status}`);
      }
    });
  }

  // Invalidate all cache after tests
  http.post(`${BASE_URL}${API_PREFIX}/cache/invalidate-all`);
  console.log('Cache invalidated');
}
