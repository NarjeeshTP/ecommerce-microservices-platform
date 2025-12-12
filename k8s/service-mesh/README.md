# Service Mesh & Resilience with Istio

Complete service mesh implementation using Istio with circuit breakers, retries, and timeout policies for production-grade resilience.

## ✅ Current Status (Dec 12, 2025)

**Version:** 0.1.0  
**Status:** Week 14 Implementation Complete

### Features Implemented
- ✅ **Istio Service Mesh** - Complete mesh installation on Kind
- ✅ **Circuit Breakers** - Automatic failure detection and isolation
- ✅ **Retry Policies** - Automatic retry on transient failures
- ✅ **Timeout Policies** - Prevent cascading delays
- ✅ **Traffic Management** - Advanced routing and load balancing
- ✅ **Observability** - Kiali, Grafana, Jaeger integration
- ✅ **Sidecar Injection** - Automatic Envoy proxy injection

---

## What is Service Mesh?

**Purpose:** Infrastructure layer that handles service-to-service communication with built-in resilience, security, and observability.

**Use:** Offload cross-cutting concerns (retries, timeouts, circuit breakers) from application code to infrastructure.

### Without Service Mesh

```java
// Application code handles everything
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Payment processPayment(Order order) {
        // Manual retry logic
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // Manual timeout
                return restTemplate.postForObject(
                    "http://payment-service/pay", 
                    order, 
                    Payment.class
                );
            } catch (Exception e) {
                if (i == maxRetries - 1) throw e;
                Thread.sleep(1000 * (i + 1)); // Backoff
            }
        }
    }
}
```

**Problems:**
- ❌ Retry logic duplicated across services
- ❌ Hard to change policies (requires code change)
- ❌ No centralized observability
- ❌ Circuit breaker needs manual implementation

### With Service Mesh (Istio)

```java
// Clean application code
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Payment processPayment(Order order) {
        // Simple call - Istio handles retry/timeout/circuit breaker
        return restTemplate.postForObject(
            "http://payment-service/pay", 
            order, 
            Payment.class
        );
    }
}
```

**Istio Configuration (separate file):**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  retries:
    attempts: 3
    perTryTimeout: 5s
  timeout: 15s
```

**Benefits:**
- ✅ Retry/timeout configured externally
- ✅ Change policies without code deployment
- ✅ Centralized in Kiali dashboard
- ✅ Works for all languages (not just Java)

---

## Architecture

### Istio Components

```
┌──────────────────────────────────────────────────────┐
│  Control Plane (istio-system namespace)             │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Istiod                                    │    │
│  │  - Pilot (traffic management)              │    │
│  │  - Citadel (certificate management)        │    │
│  │  - Galley (configuration validation)       │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ┌────────────────────────────────────────────┐    │
│  │  Istio Ingress Gateway                     │    │
│  │  - External traffic entry                  │    │
│  └────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
                     │
                     ↓ Configuration
┌──────────────────────────────────────────────────────┐
│  Data Plane (application namespaces)                │
│                                                      │
│  ┌────────────────────┐  ┌────────────────────┐    │
│  │ Order Service Pod  │  │ Payment Service    │    │
│  │ ┌────────────────┐ │  │ ┌────────────────┐│    │
│  │ │ App Container  │ │  │ │ App Container  ││    │
│  │ └────────────────┘ │  │ └────────────────┘│    │
│  │ ┌────────────────┐ │  │ ┌────────────────┐│    │
│  │ │ Envoy Sidecar  │←┼──┼→│ Envoy Sidecar  ││    │
│  │ │ (Proxy)        │ │  │ │ (Proxy)        ││    │
│  │ └────────────────┘ │  │ └────────────────┘│    │
│  └────────────────────┘  └────────────────────┘    │
│                                                      │
│  All traffic goes through Envoy sidecars            │
└──────────────────────────────────────────────────────┘
```

**How It Works:**
1. **Sidecar Injection**: Istio automatically injects Envoy proxy container into each pod
2. **Traffic Interception**: All network traffic goes through the sidecar
3. **Policy Enforcement**: Sidecar enforces retries, timeouts, circuit breakers
4. **Telemetry**: Sidecar reports metrics, traces, logs

---

## Installation

### Prerequisites

```bash
# Install Istio CLI
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

# Verify
istioctl version

# Kubernetes cluster
./k8s/scripts/setup-kind.sh  # Must be running
```

### Install Istio

```bash
# Run installation script
./k8s/service-mesh/scripts/install-istio.sh

# What it does:
# 1. Installs Istio control plane (istiod)
# 2. Installs Istio ingress gateway
# 3. Labels namespaces for sidecar injection
# 4. Installs addons (Kiali, Prometheus, Grafana, Jaeger)
```

### Verify Installation

```bash
# Check Istio components
kubectl get pods -n istio-system

# Expected output:
# NAME                                    READY   STATUS
# istio-ingressgateway-xxx                1/1     Running
# istiod-xxx                              1/1     Running
# kiali-xxx                               1/1     Running
# prometheus-xxx                          1/1     Running
# grafana-xxx                             1/1     Running
# jaeger-xxx                              1/1     Running

# Check namespace labels
kubectl get namespace -L istio-injection

# Expected:
# NAME              STATUS   ISTIO-INJECTION
# platform-core     Active   enabled
# platform-system   Active   enabled
# platform-infra    Active   enabled
```

---

## Sidecar Injection

**Purpose:** Automatically inject Envoy proxy into every pod for traffic interception.

**Use:** Enable service mesh features without modifying application code.

### Automatic Injection

```bash
# Label namespace (already done by install script)
kubectl label namespace platform-core istio-injection=enabled

# Restart deployments to inject sidecars
kubectl rollout restart deployment -n platform-core

# Check pods (should have 2/2 containers)
kubectl get pods -n platform-core

# Expected:
# NAME                              READY   STATUS
# catalog-service-xxx               2/2     Running
# order-service-xxx                 2/2     Running
# payment-service-xxx               2/2     Running
```

**2/2 Containers:**
- Container 1: Your application
- Container 2: Envoy sidecar (injected by Istio)

### Manual Injection (Optional)

```bash
# Inject into deployment YAML
istioctl kube-inject -f deployment.yaml | kubectl apply -f -

# Or inject into pod
kubectl apply -f <(istioctl kube-inject -f pod.yaml)
```

---

## Circuit Breaker

**Purpose:** Detect failing instances and remove them from load balancing pool.

**Use:** Prevent cascading failures when a service instance becomes unhealthy.

### Configuration

**File:** `k8s/service-mesh/resilience-policies/circuit-breaker.yaml`

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: order-service-circuit-breaker
  namespace: platform-core
spec:
  host: order-service.platform-core.svc.cluster.local
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100        # Max TCP connections
      http:
        http1MaxPendingRequests: 50  # Max pending requests
        http2MaxRequests: 100        # Max concurrent requests
        maxRequestsPerConnection: 2   # Limit per connection
    outlierDetection:
      consecutiveErrors: 5           # Open after 5 errors
      interval: 30s                  # Check every 30s
      baseEjectionTime: 30s          # Eject for 30s
      maxEjectionPercent: 50         # Max 50% of instances
      minHealthPercent: 50           # Min 50% must be healthy
```

**How It Works:**

```
1. Request to order-service
2. If instance returns 5 consecutive errors
3. Circuit breaker OPENS
4. Instance ejected from load balancing for 30s
5. Traffic redirected to healthy instances
6. After 30s, instance added back (half-open state)
7. If still failing → eject again
8. If healthy → circuit breaker CLOSES
```

**States:**
- **CLOSED**: Normal operation, traffic flows
- **OPEN**: Instance ejected, no traffic sent
- **HALF-OPEN**: Testing if instance recovered

### Apply Circuit Breaker

```bash
# Apply to all services
kubectl apply -f k8s/service-mesh/resilience-policies/circuit-breaker.yaml

# Verify
kubectl get destinationrules -n platform-core

# Check status in Kiali
istioctl dashboard kiali
```

### Test Circuit Breaker

```bash
# Inject failures with Chaos Service
curl -X POST http://localhost:8092/api/chaos/error \
  -H "Content-Type: application/json" \
  -d '{
    "service": "order-service",
    "errorRate": 1.0,
    "errorType": "HTTP_500"
  }'

# Generate load
for i in {1..100}; do
  curl http://localhost:8090/api/orders
done

# Check Kiali for circuit breaker activation
# Graph shows:
# - Failed requests (red)
# - Circuit breaker open (yellow warning icon)
# - Traffic rerouted to healthy instances
```

---

## Retry Policy

**Purpose:** Automatically retry failed requests to handle transient failures.

**Use:** Improve reliability without changing application code.

### Configuration

**File:** `k8s/service-mesh/resilience-policies/retry-timeout.yaml`

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service-retry
  namespace: platform-core
spec:
  hosts:
  - order-service.platform-core.svc.cluster.local
  http:
  - route:
    - destination:
        host: order-service.platform-core.svc.cluster.local
        port:
          number: 8084
    retries:
      attempts: 3                    # Retry 3 times
      perTryTimeout: 2s              # Each attempt timeout
      retryOn: 5xx,reset,connect-failure,refused-stream
    timeout: 10s                     # Total timeout
```

**Retry Conditions:**
- `5xx` - Server errors (500, 503)
- `reset` - Connection reset
- `connect-failure` - Cannot connect
- `refused-stream` - HTTP/2 stream refused

**How It Works:**

```
Request → Attempt 1 (fails with 503)
       → Attempt 2 (fails with 503)
       → Attempt 3 (succeeds with 200)
Result: Success after 3 attempts
```

**Without Retry:**
```
Request → Attempt 1 (fails with 503)
Result: Failure (user sees error)
```

### Exponential Backoff

Istio uses exponential backoff between retries:

```
Attempt 1: Immediate
Attempt 2: After 25ms
Attempt 3: After 50ms
Attempt 4: After 100ms
```

### Apply Retry Policy

```bash
# Apply to all services
kubectl apply -f k8s/service-mesh/resilience-policies/retry-timeout.yaml

# Verify
kubectl get virtualservices -n platform-core

# Test
curl -X POST http://localhost:8092/api/chaos/error \
  -d '{"service": "order-service", "errorRate": 0.3}'

# 30% of requests fail on first attempt
# Istio retries automatically
# Success rate improves to ~99%
```

---

## Timeout Policy

**Purpose:** Limit how long to wait for a response to prevent cascading delays.

**Use:** Fail fast instead of waiting indefinitely for slow services.

### Configuration

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  http:
  - route:
    - destination:
        host: payment-service
    timeout: 15s           # Total request timeout
    retries:
      perTryTimeout: 5s    # Each retry attempt timeout
```

**How It Works:**

```
Request sent at T=0
├─ Attempt 1: 0-5s (timeout at 5s)
├─ Attempt 2: 5-10s (timeout at 10s)
├─ Attempt 3: 10-15s (timeout at 15s)
└─ Total timeout: 15s
```

**Without Timeout:**
```
Request sent at T=0
└─ Waiting... (could wait forever)
    ├─ Database slow (30s)
    ├─ Network issue (2 minutes)
    └─ Service deadlock (infinite)
```

### Timeout Best Practices

| Service | Total Timeout | Per-Try Timeout | Reason |
|---------|---------------|-----------------|--------|
| **Catalog** | 5s | 1s | Read-only, cache-backed |
| **Order** | 10s | 2s | Creates resources |
| **Payment** | 15s | 5s | External API call |
| **Inventory** | 8s | 2s | High concurrency |

### Apply Timeout Policy

```bash
# Already included in retry-timeout.yaml
kubectl apply -f k8s/service-mesh/resilience-policies/retry-timeout.yaml

# Test with latency injection
curl -X POST http://localhost:8092/api/chaos/latency \
  -d '{
    "service": "order-service",
    "delayMs": 20000,
    "probability": 1.0
  }'

# Request times out after 10s (not 20s)
# Error: upstream request timeout
```

---

## Traffic Management

### Load Balancing

**Default:** Round-robin across healthy instances

**Custom Load Balancing:**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
spec:
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN  # Route to least busy instance
      # Options: ROUND_ROBIN, LEAST_CONN, RANDOM, PASSTHROUGH
```

### Canary Deployment

**Purpose:** Gradually roll out new version.

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  http:
  - match:
    - headers:
        x-version:
          exact: "v2"
    route:
    - destination:
        host: order-service
        subset: v2
  - route:
    - destination:
        host: order-service
        subset: v1
      weight: 90
    - destination:
        host: order-service
        subset: v2
      weight: 10  # 10% to new version
```

---

## Observability

### Kiali Dashboard

**Purpose:** Visualize service mesh traffic and health.

**Use:** Monitor circuit breakers, retries, latencies in real-time.

```bash
# Open Kiali
istioctl dashboard kiali

# Or port forward
kubectl port-forward -n istio-system svc/kiali 20001:20001
open http://localhost:20001
```

**Features:**
- **Service Graph**: Visual representation of service communication
- **Circuit Breaker Status**: See which services have open circuits
- **Retry Statistics**: Number of retries per service
- **Traffic Metrics**: Request rate, latency, errors

### Grafana Dashboards

```bash
# Open Grafana
istioctl dashboard grafana

# Pre-built dashboards:
# - Istio Service Dashboard
# - Istio Workload Dashboard
# - Istio Performance Dashboard
```

### Jaeger Tracing

```bash
# Open Jaeger
istioctl dashboard jaeger

# See distributed traces with Istio spans:
# - Retry attempts
# - Circuit breaker decisions
# - Timeout enforcement
```

---

## Testing Resilience

### Scenario 1: Circuit Breaker

```bash
# 1. Enable error injection
curl -X POST http://localhost:8092/api/chaos/error \
  -d '{
    "service": "payment-service",
    "errorRate": 1.0
  }'

# 2. Generate load
for i in {1..50}; do
  curl http://localhost:8090/api/orders
done

# 3. Check Kiali
# - Circuit breaker opens after 3 errors
# - Traffic stops going to payment-service
# - Order service receives errors immediately

# 4. Disable chaos
curl -X DELETE http://localhost:8092/api/chaos/scenarios/payment-service

# 5. Wait 60s (baseEjectionTime)
sleep 60

# 6. Circuit breaker closes
# - Traffic resumes to payment-service
```

### Scenario 2: Retry

```bash
# 1. Enable 50% error rate
curl -X POST http://localhost:8092/api/chaos/error \
  -d '{
    "service": "catalog-service",
    "errorRate": 0.5
  }'

# 2. Make requests
curl http://localhost:8090/api/catalog/products

# Result:
# - First attempt: 50% chance of failure
# - Retry attempt 1: 50% chance
# - Retry attempt 2: 50% chance
# - Final success rate: ~87.5%

# Check Jaeger trace:
# - Shows multiple spans for retries
# - Final span successful
```

### Scenario 3: Timeout

```bash
# 1. Enable 20-second latency
curl -X POST http://localhost:8092/api/chaos/latency \
  -d '{
    "service": "inventory-service",
    "delayMs": 20000
  }'

# 2. Make request
time curl http://localhost:8090/api/inventory/check

# Result:
# - Request times out after 8s (not 20s)
# - Error: upstream request timeout
# - Prevents cascading delays
```

---

## Comparison: Client-Side vs Mesh-Side Resilience

| Feature | Client-Side (Spring) | Mesh-Side (Istio) |
|---------|---------------------|-------------------|
| **Implementation** | Java code | YAML config |
| **Language Support** | Java only | All languages |
| **Deployment** | Requires app restart | No restart needed |
| **Observability** | Limited | Built-in (Kiali) |
| **Circuit Breaker** | Manual (Resilience4j) | Automatic (Envoy) |
| **Retry** | Manual | Automatic |
| **Timeout** | Manual | Automatic |
| **Best For** | Single language platform | Polyglot services |

**Recommendation:**
- Use **Istio** for cross-cutting concerns (retry, timeout, circuit breaker)
- Use **application code** for business logic retries (e.g., payment idempotency)

---

## Troubleshooting

### Sidecar Not Injected

```bash
# Check namespace label
kubectl get namespace platform-core -o yaml | grep istio-injection

# Re-label if needed
kubectl label namespace platform-core istio-injection=enabled --overwrite

# Restart deployment
kubectl rollout restart deployment/catalog-service -n platform-core
```

### Circuit Breaker Not Working

```bash
# Check DestinationRule
kubectl get destinationrule catalog-service-circuit-breaker -n platform-core -o yaml

# Check if service has multiple instances
kubectl get pods -n platform-core -l app=catalog-service

# Need at least 2 instances for circuit breaker to work
```

### Retries Not Happening

```bash
# Check VirtualService
kubectl get virtualservice catalog-service-retry -n platform-core -o yaml

# Check Envoy logs
kubectl logs catalog-service-xxx -c istio-proxy -n platform-core

# Look for:
# - "upstream_rq_retry"
# - "upstream_rq_timeout"
```

---

## Best Practices

### Circuit Breaker Tuning

```yaml
# Start conservative
consecutiveErrors: 5
interval: 30s
baseEjectionTime: 30s

# Tune based on monitoring
# - Too sensitive: Frequent false positives
# - Too lenient: Slow failure detection
```

### Retry Configuration

```yaml
# Don't retry:
# - POST/PUT (not idempotent)
# - Payment operations
# - Long operations (>10s)

# Do retry:
# - GET requests
# - Transient failures (5xx, connection errors)
# - Short operations (<5s)
```

### Timeout Values

```yaml
# Formula: timeout = (attempts × perTryTimeout) + overhead
# Example: 10s = (3 × 2s) + 4s buffer

# Guidelines:
# - perTryTimeout < timeout / attempts
# - timeout < upstream SLA
# - Consider retry overhead
```

---

## Week 14 Summary

### Completed
- ✅ Istio service mesh installation
- ✅ Sidecar injection for all namespaces
- ✅ Circuit breaker policies (3 services)
- ✅ Retry policies (4 services)
- ✅ Timeout policies (all services)
- ✅ Observability integration (Kiali, Grafana, Jaeger)
- ✅ Traffic management configuration
- ✅ Resilience testing procedures

### Key Achievements
- **Zero Code Changes**: All resilience added via configuration
- **Production-Ready**: Circuit breakers, retries, timeouts active
- **Observable**: Kiali dashboard shows all traffic patterns
- **Testable**: Chaos engineering validates resilience

### Next Steps
- Week 15: CI/CD pipelines
- Week 16: SLO monitoring and alerting

---

**Last Updated:** December 12, 2025  
**Version:** 0.1.0  
**Status:** Week 14 Complete ✅

