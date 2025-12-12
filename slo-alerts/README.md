# SLOs & Alerting Guide

Complete Service Level Objectives (SLOs), Prometheus alerts, Grafana dashboards, incident runbooks, and chaos experiments for E-Commerce Microservices Platform.

## ✅ Current Status (Dec 12, 2025)

**Version:** 0.1.0  
**Status:** Week 16 Implementation Complete

### Features Implemented
- ✅ **SLO Definitions** - Availability, latency, error budget
- ✅ **Prometheus Alert Rules** - 25+ alerts for SLO monitoring
- ✅ **Error Budget Tracking** - Fast and slow burn rate alerts
- ✅ **Incident Runbooks** - Step-by-step recovery procedures
- ✅ **Grafana Dashboards** - SLO visualization and tracking
- ✅ **Chaos Experiments** - Validation and rollback procedures
- ✅ **Business Metrics** - Revenue and conversion tracking

---

## What are SLOs?

**Service Level Objective (SLO):** Target level of reliability for a service.

**Purpose:** Define clear expectations for service reliability and user experience.

**Use:** Measure actual performance against targets, make informed trade-offs between feature velocity and reliability.

### SLO Hierarchy

```
SLI (Service Level Indicator)
  ↓
  Measurement of service behavior
  Example: HTTP request success rate

SLO (Service Level Objective)
  ↓
  Target for an SLI
  Example: 99.9% success rate

SLA (Service Level Agreement)
  ↓
  Contractual commitment (usually SLO - buffer)
  Example: 99.5% availability (with penalties)

Error Budget
  ↓
  Allowed failures = (1 - SLO) × Total requests
  Example: 0.1% × 1M requests = 1,000 failed requests allowed/month
```

---

## Our SLOs

### 1. Availability SLO

**Target:** 99.9% availability (three nines)

**Meaning:**
- Maximum downtime: 43.2 minutes per month
- Out of 1 million requests, max 1,000 can fail

**Measurement:**
```promql
(
  sum(rate(http_server_requests_seconds_count{status!~"5.."}[30d]))
  /
  sum(rate(http_server_requests_seconds_count[30d]))
)
```

**Error Budget:**
- Monthly: 1,000 errors per 1M requests
- Daily: ~33 errors per 33K requests
- Hourly: ~1.4 errors per 1.4K requests

**When Budget Exhausted:**
- ❌ Freeze feature releases
- ✅ Focus on reliability improvements
- ✅ Only deploy critical bug fixes

---

### 2. Latency SLO

**Target:** 
- 95% of requests < 500ms (P95)
- 99% of requests < 2s (P99)

**Meaning:**
- 95 out of 100 requests complete in under 500ms
- 99 out of 100 requests complete in under 2 seconds

**Measurement:**
```promql
# P95 latency
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)

# P99 latency
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)
```

**Impact:**
- **500ms:** User feels instant response
- **1s:** User notices slight delay
- **2s:** User perceives slowness
- **>3s:** High abandonment risk

---

### 3. Critical User Journey SLOs

#### Checkout Flow

**SLO:** 99% success rate, P95 < 3 seconds

**Steps:**
1. Add to cart (catalog + pricing) - 200ms
2. View cart (cart-service) - 150ms
3. Create order (order-service) - 500ms
4. Process payment (payment-service) - 1.5s
5. Confirm order (notification-service) - 100ms

**Total:** ~2.5s end-to-end

**Error Budget:** 1% of checkouts can fail
- If 1000 checkouts/day → 10 failures allowed

#### Search to Product

**SLO:** 99.5% success rate, P95 < 300ms

**Measurement:** Search query to product page load

---

## Error Budget

### What is Error Budget?

**Error Budget = 1 - SLO**

For 99.9% availability SLO:
- Error Budget = 0.1%
- In 30 days with 1M requests: 1,000 requests can fail

**Purpose:**
- ✅ Quantify acceptable failures
- ✅ Balance feature velocity vs reliability
- ✅ Make data-driven deployment decisions

### Error Budget Burn Rate

**Fast Burn:** Rapid budget consumption (outage scenario)
```
5% error rate for 5 minutes
= Burns 1% of monthly budget in 5 minutes
= At this rate, budget exhausted in 8 hours
→ Page on-call immediately
```

**Slow Burn:** Gradual budget consumption (degradation scenario)
```
0.5% error rate for 1 hour
= Burns ~10% of monthly budget in 1 hour
= At this rate, budget exhausted in 10 hours
→ Investigate during business hours
```

### Error Budget Policy

```
Budget Remaining | Action
─────────────────|────────────────────────────────────
> 50%            | Normal operations
                 | Deploy features freely
                 | Experiment with new tech
─────────────────|────────────────────────────────────
25-50%           | Caution mode
                 | Increase testing rigor
                 | Review deployment frequency
─────────────────|────────────────────────────────────
10-25%           | Freeze non-critical changes
                 | Focus on reliability
                 | Deploy only critical fixes
─────────────────|────────────────────────────────────
< 10%            | Full freeze
                 | Emergency fixes only
                 | Post-mortem required
─────────────────|────────────────────────────────────
0%               | Total freeze
                 | No deployments
                 | Incident review
```

---

## Prometheus Alert Rules

### Alert Categories

1. **SLO Availability** - Error rate and uptime
2. **SLO Latency** - P95, P99 thresholds
3. **Error Budget** - Burn rate (fast/slow)
4. **Critical Journeys** - Checkout, payment
5. **Dependencies** - Database, Kafka, Redis
6. **Business Metrics** - Revenue, conversion
7. **Saturation** - CPU, memory, disk

### Key Alerts

#### 1. Fast Error Budget Burn

**Trigger:** 2% error rate for 5 minutes

**Severity:** Critical (page on-call)

```yaml
- alert: SLOErrorBudgetBurnFast
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5..", job=~".*-service"}[5m]))
      /
      sum(rate(http_server_requests_seconds_count{job=~".*-service"}[5m]))
    ) > 0.02
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Fast error budget burn detected"
    description: "Burning through monthly error budget at {{ $value | humanizePercentage }} error rate."
    action: "Page on-call engineer immediately"
```

**What to Do:**
1. Check runbook: [availability-slo.md](#availability-slo-runbook)
2. Identify failing service(s)
3. Roll back recent deployment
4. Investigate root cause

#### 2. Checkout Journey Failure

**Trigger:** Order creation error rate > 1%

**Severity:** Critical (revenue impact)

```yaml
- alert: CheckoutJourneyFailureRateHigh
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{uri="/api/orders", status=~"5.."}[5m]))
      /
      sum(rate(http_server_requests_seconds_count{uri="/api/orders"}[5m]))
    ) > 0.01
  for: 2m
  labels:
    severity: critical
    journey: checkout
  annotations:
    summary: "Checkout failure rate exceeds SLO"
    description: "Order creation failing at {{ $value | humanizePercentage }}. Revenue impact!"
    runbook: "runbooks/checkout-failure.md"
```

#### 3. P95 Latency Breached

**Trigger:** P95 latency > 500ms for 5 minutes

**Severity:** Warning

```yaml
- alert: SLOLatencyP95Breached
  expr: |
    histogram_quantile(0.95,
      sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
    ) > 0.5
  for: 5m
  labels:
    severity: warning
    slo: latency
  annotations:
    summary: "P95 latency SLO breached for {{ $labels.job }}"
    description: "P95 latency is {{ $value }}s (threshold: 0.5s)."
    runbook: "runbooks/latency-slo.md"
```

---

## Grafana Dashboards

### SLO Overview Dashboard

**Panels:**

1. **Availability SLO**
   - Current: 99.95%
   - Target: 99.9%
   - Status: ✅ Meeting SLO

2. **Error Budget**
   - Remaining: 78%
   - Burn Rate: 0.5%/hour
   - Status: ✅ Healthy

3. **Latency SLO**
   - P95: 320ms (target: 500ms)
   - P99: 1.2s (target: 2s)
   - Status: ✅ Meeting SLO

4. **Critical Journeys**
   - Checkout Success: 99.3%
   - Payment Success: 99.7%
   - Status: ✅ Above target

### Error Budget Dashboard

**30-Day View:**
```
Error Budget Tracking
────────────────────────────────────────────────────
100% ┤                  ╭────────────────
     │                ╱
 75% ┤              ╱
     │            ╱
 50% ┤          ╱
     │        ╱
 25% ┤      ╱
     │    ╱
  0% ┼──╯───────────────────────────────────
     0   5   10  15  20  25  30 (days)

Current: 78% remaining
Trend: Stable (0.7% burn/day)
Forecast: 52% remaining at month-end
```

---

## Incident Runbooks

### <a name="availability-slo-runbook"></a>Availability SLO Breach

**Trigger:** Error rate > 5% for 2 minutes

**Severity:** Critical

**Symptoms:**
- High 5xx error rate
- SLO dashboard showing red
- User complaints about errors

**Investigation Steps:**

1. **Identify Scope**
   ```bash
   # Check error rate by service
   kubectl top pods -n platform-core
   
   # Check logs for errors
   kubectl logs -n platform-core -l app=order-service --tail=100 | grep ERROR
   ```

2. **Check Recent Deployments**
   ```bash
   # List recent deployments
   helm list -n platform-core --date
   
   # Check last deploy time
   kubectl rollout history deployment/order-service -n platform-core
   ```

3. **Identify Root Cause**
   - [ ] Recent deployment?
   - [ ] Database connection issues?
   - [ ] External service outage?
   - [ ] Resource exhaustion?
   - [ ] Traffic spike?

**Mitigation:**

```bash
# Option 1: Rollback last deployment
helm rollback order-service -n platform-core

# Option 2: Scale up if resource constrained
kubectl scale deployment/order-service --replicas=5 -n platform-core

# Option 3: Enable circuit breaker manually
kubectl apply -f k8s/service-mesh/resilience-policies/circuit-breaker.yaml
```

**Verification:**
```bash
# Check error rate dropped
watch kubectl get pods -n platform-core

# Monitor SLO dashboard
# Grafana → SLO Overview → Error Rate Panel
```

**Communication:**
- Update status page
- Post in #incidents Slack channel
- Notify on-call manager if not resolved in 15 min

---

### Checkout Failure Runbook

**Trigger:** Order creation error rate > 1%

**Severity:** Critical (revenue impact)

**Investigation:**

1. **Check Order Service**
   ```bash
   # Service health
   kubectl get pods -n platform-core -l app=order-service
   
   # Recent logs
   kubectl logs -n platform-core -l app=order-service --tail=50 --timestamps
   ```

2. **Check Dependencies**
   ```bash
   # Database connectivity
   kubectl run pg-test --rm -it --image=postgres:15 -- \
     psql -h postgres-service.platform-infra -U postgres -c "SELECT 1"
   
   # Kafka connectivity
   kubectl run kafka-test --rm -it --image=confluentinc/cp-kafka -- \
     kafka-topics --list --bootstrap-server kafka:9092
   ```

3. **Check Payment Service**
   ```bash
   # Payment service status
   curl -X POST http://payment-service.platform-core:8085/api/payments/health
   ```

**Common Issues:**

| Symptom | Cause | Fix |
|---------|-------|-----|
| **Database timeout** | Connection pool exhausted | Scale order-service pods |
| **Kafka lag** | Consumer not processing | Restart consumer pods |
| **Payment gateway down** | External API outage | Enable fallback mode |
| **OOM kills** | Memory leak | Restart pods, increase limits |

**Rollback:**
```bash
# Rollback order-service
helm rollback order-service -n platform-core

# Verify
curl http://order-service.platform-core:8084/actuator/health
```

---

### Latency SLO Breach

**Trigger:** P95 latency > 500ms

**Severity:** Warning

**Investigation:**

1. **Identify Slow Service**
   ```bash
   # Check latency by service (Jaeger)
   open http://localhost:16686
   # Search for slow traces
   
   # Prometheus query
   histogram_quantile(0.95,
     rate(http_server_requests_seconds_bucket[5m])
   ) by (job)
   ```

2. **Check Resource Saturation**
   ```bash
   # CPU usage
   kubectl top pods -n platform-core
   
   # Memory usage
   kubectl top nodes
   ```

3. **Check Dependencies**
   - Database slow queries
   - Cache miss rate high
   - External API slow

**Mitigation:**

```bash
# Scale up if CPU high
kubectl scale deployment/catalog-service --replicas=5 -n platform-core

# Increase cache TTL
kubectl edit configmap catalog-config -n platform-core
# Update: cache.ttl=600 (10 minutes)
kubectl rollout restart deployment/catalog-service -n platform-core

# Add database read replicas (if DB slow)
# Contact DBA team
```

---

### Database Slow Queries

**Trigger:** Connection timeouts > 0.01/sec

**Severity:** Critical

**Investigation:**

1. **Check Active Queries**
   ```sql
   SELECT pid, query, state, wait_event, query_start
   FROM pg_stat_activity
   WHERE state != 'idle'
   ORDER BY query_start;
   ```

2. **Identify Slow Queries**
   ```sql
   SELECT query, mean_exec_time, calls
   FROM pg_stat_statements
   ORDER BY mean_exec_time DESC
   LIMIT 10;
   ```

3. **Check Connection Pool**
   ```bash
   # HikariCP metrics
   curl http://catalog-service:8080/actuator/metrics/hikaricp.connections.active
   ```

**Mitigation:**

```bash
# Kill long-running query
psql -c "SELECT pg_terminate_backend(12345);"

# Increase connection pool (temporary)
kubectl set env deployment/catalog-service SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20

# Add database index (if missing)
psql -c "CREATE INDEX idx_products_name ON products(name);"
```

---

## Chaos Experiments

### Purpose

**What:** Deliberately inject failures to test system resilience

**Why:** 
- Validate SLOs under failure conditions
- Test runbooks and alerting
- Build confidence in recovery procedures

### Experiment 1: Service Outage

**Hypothesis:** Order service failure triggers circuit breaker, other services continue operating

**Procedure:**

```bash
# 1. Enable 100% error rate on order-service
curl -X POST http://localhost:8092/api/chaos/error \
  -H "Content-Type: application/json" \
  -d '{
    "service": "order-service",
    "errorRate": 1.0,
    "errorType": "HTTP_503",
    "durationSeconds": 300
  }'

# 2. Monitor alerts (should fire within 2 minutes)
# - SLOAvailabilityBudgetBurnRateCritical
# - CheckoutJourneyFailureRateHigh

# 3. Verify circuit breaker opens
kubectl logs -n istio-system -l app=istiod --tail=50 | grep "outlier_detection"

# 4. Check other services still healthy
curl http://catalog-service.platform-core:8080/actuator/health
curl http://cart-service.platform-core:8083/actuator/health

# 5. Rollback (simulate recovery)
curl -X DELETE http://localhost:8092/api/chaos/scenarios/order-service

# 6. Verify alerts clear
# Grafana → Alerting → Should see resolved

# 7. Check error budget impact
# Grafana → Error Budget Dashboard
# Should show budget consumed
```

**Expected Results:**
- ✅ Alert fires within 2 minutes
- ✅ Circuit breaker opens
- ✅ Other services unaffected
- ✅ System recovers automatically
- ✅ Error budget decreased by ~5%

---

### Experiment 2: Latency Injection

**Hypothesis:** High latency triggers timeout policies, requests fail fast

**Procedure:**

```bash
# 1. Inject 5-second latency on payment-service
curl -X POST http://localhost:8092/api/chaos/latency \
  -d '{
    "service": "payment-service",
    "delayMs": 5000,
    "probability": 1.0
  }'

# 2. Make order request
time curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"productId": "LAPTOP-001", "quantity": 1}]}'

# Expected: Timeout after 2 seconds (per Istio timeout policy)
# Actual: Should see "upstream request timeout"

# 3. Verify latency alert fires
# - SLOLatencyP95Breached (if sustained)

# 4. Clean up
curl -X DELETE http://localhost:8092/api/chaos/scenarios/payment-service
```

**Expected Results:**
- ✅ Request times out after 2s (not 5s)
- ✅ Error returned to client quickly
- ✅ No cascading delays to other services
- ✅ P95 latency alert may fire

---

### Experiment 3: Resource Exhaustion

**Hypothesis:** Auto-scaling triggers when CPU/memory saturated

**Procedure:**

```bash
# 1. Generate high load on catalog-service
kubectl run load-generator --image=williamyeh/hey:latest --rm -it -- \
  -z 5m -c 50 -q 100 \
  http://catalog-service.platform-core:8080/api/products

# 2. Monitor resource usage
watch kubectl top pods -n platform-core -l app=catalog-service

# 3. Verify HPA scales up
kubectl get hpa -n platform-core catalog-service-hpa -w

# Expected: Scales from 2 → 5 pods as CPU exceeds 70%

# 4. Wait for load test to complete

# 5. Verify scale down after 5 minutes
# Expected: Scales back down to 2 pods
```

**Expected Results:**
- ✅ HPA scales up within 30 seconds
- ✅ CPU/memory stay below critical thresholds
- ✅ Latency remains under SLO
- ✅ HPA scales down after load subsides

---

### Experiment 4: Database Connection Pool Exhaustion

**Hypothesis:** Connection pool exhaustion triggers alerts, service degrades gracefully

**Procedure:**

```bash
# 1. Reduce connection pool size
kubectl set env deployment/catalog-service \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5 \
  -n platform-core

# 2. Generate high concurrency
for i in {1..100}; do
  curl http://catalog-service.platform-core:8080/api/products &
done

# 3. Monitor connection pool metrics
curl http://catalog-service.platform-core:8080/actuator/metrics/hikaricp.connections.active

# 4. Verify alert fires
# - DatabaseConnectionPoolExhausted

# 5. Restore normal pool size
kubectl set env deployment/catalog-service \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20 \
  -n platform-core
```

**Expected Results:**
- ✅ Alert fires when pool > 90% full
- ✅ Some requests time out (fail fast)
- ✅ No cascading failures
- ✅ System recovers after pool size increased

---

## Rollback Procedures

### Helm Rollback

```bash
# List release history
helm history order-service -n platform-core

# Output:
# REVISION  UPDATED                   STATUS      DESCRIPTION
# 1         Mon Dec 11 10:00:00 2025  superseded  Install complete
# 2         Tue Dec 12 09:00:00 2025  superseded  Upgrade complete
# 3         Tue Dec 12 14:00:00 2025  deployed    Upgrade complete

# Rollback to revision 2
helm rollback order-service 2 -n platform-core

# Verify
kubectl rollout status deployment/order-service -n platform-core
```

### Kubernetes Rollback

```bash
# Check rollout history
kubectl rollout history deployment/order-service -n platform-core

# Rollback to previous version
kubectl rollout undo deployment/order-service -n platform-core

# Rollback to specific revision
kubectl rollout undo deployment/order-service --to-revision=2 -n platform-core

# Monitor rollback
kubectl rollout status deployment/order-service -n platform-core
```

### Canary Rollback

```bash
# If using Istio VirtualService for canary

# Remove canary route (send 100% to stable)
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service
  namespace: platform-core
spec:
  hosts:
  - order-service
  http:
  - route:
    - destination:
        host: order-service
        subset: stable
      weight: 100
EOF

# Delete canary deployment
kubectl delete deployment order-service-canary -n platform-core
```

---

## Best Practices

### 1. Alert Fatigue Prevention

**Problem:** Too many alerts → ignored alerts → missed incidents

**Solution:**
- ✅ Only alert on SLO breaches (not every error)
- ✅ Use multi-window burn rate alerts
- ✅ Tune alert thresholds based on historical data
- ✅ Route warnings to Slack, criticals to PagerDuty
- ✅ Silence known issues during maintenance

### 2. Runbook Maintenance

**Guidelines:**
- Update runbooks after each incident
- Include screenshots and example commands
- Test runbooks quarterly (chaos experiments)
- Keep runbooks in version control
- Link runbooks from alerts

### 3. Error Budget Management

**Monthly Review:**
- Calculate actual vs budgeted error rate
- Identify top contributors to budget consumption
- Adjust deployment frequency if needed
- Report to stakeholders

**Quarterly:**
- Review SLO targets (too strict? too lenient?)
- Update error budget policy
- Assess alert effectiveness

### 4. Chaos Engineering

**Schedule:**
- Weekly: Small experiments (service-level)
- Monthly: Medium experiments (cross-service)
- Quarterly: Large experiments (regional failover)

**Safety:**
- Always run in staging first
- Have rollback ready
- Communicate experiment window
- Monitor closely during experiment

---

## Week 16 Summary

### Completed
- ✅ SLO definitions (availability, latency, error budget)
- ✅ 25+ Prometheus alert rules
- ✅ Error budget tracking (fast/slow burn)
- ✅ Incident runbooks (5 scenarios)
- ✅ Grafana SLO dashboards
- ✅ Chaos experiments (4 scenarios)
- ✅ Rollback procedures
- ✅ Best practices guide

### Key Achievements
- **Production-Ready Alerting**: Multi-window burn rate alerts
- **Clear Runbooks**: Step-by-step recovery procedures
- **Chaos Validated**: System resilience tested under failures
- **Business Alignment**: SLOs tied to user experience and revenue

### Next Steps
- Week 17: Advanced topics (schema governance, contract testing)
- Week 18-24: Remaining services and production launch

---

**Last Updated:** December 12, 2025  
**Version:** 0.1.0  
**Status:** Week 16 Complete ✅

