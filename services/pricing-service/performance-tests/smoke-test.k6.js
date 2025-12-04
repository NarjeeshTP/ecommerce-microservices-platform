import http from 'k6/http';
import { check, sleep } from 'k6';

// Smoke test configuration - minimal load to verify the system works
export const options = {
  vus: 1,          // 1 virtual user
  duration: '30s', // Run for 30 seconds
  thresholds: {
    'http_req_duration': ['p(95)<1000'], // 95% of requests should be below 1s
    'http_req_failed': ['rate<0.05'],     // Error rate should be less than 5%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const API_PREFIX = '/api/v1/pricing';

export function setup() {
  console.log('Starting smoke test...');
  
  // Create a test pricing rule
  const payload = JSON.stringify({
    itemId: 'SMOKE-TEST-001',
    basePrice: 999.99,
    discountPercent: 10.00,
    currency: 'USD',
    ruleType: 'PROMOTIONAL',
    status: 'ACTIVE'
  });

  const res = http.post(`${BASE_URL}${API_PREFIX}/rules`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status === 201) {
    const rule = JSON.parse(res.body);
    console.log(`Created test pricing rule with ID: ${rule.id}`);
    return { ruleId: rule.id };
  } else {
    console.error(`Failed to create test pricing rule: ${res.status}`);
    return { ruleId: null };
  }
}

export default function (data) {
  // Test 1: Get price
  let res = http.get(`${BASE_URL}${API_PREFIX}/price/SMOKE-TEST-001`);
  check(res, {
    'get price status is 200': (r) => r.status === 200,
    'get price has correct item': (r) => JSON.parse(r.body).itemId === 'SMOKE-TEST-001',
  });
  
  sleep(1);
  
  // Test 2: Get all rules
  res = http.get(`${BASE_URL}${API_PREFIX}/rules`);
  check(res, {
    'get all rules status is 200': (r) => r.status === 200,
    'get all rules returns array': (r) => Array.isArray(JSON.parse(r.body)),
  });
  
  sleep(1);
}

export function teardown(data) {
  if (data.ruleId) {
    const res = http.del(`${BASE_URL}${API_PREFIX}/rules/${data.ruleId}`);
    if (res.status === 204) {
      console.log('Cleaned up test data');
    }
  }
}
