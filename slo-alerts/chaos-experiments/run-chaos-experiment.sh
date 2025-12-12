#!/bin/bash
set -e
# Chaos Experiment Runner with SLO Validation
# Usage: ./run-chaos-experiment.sh [experiment-name]
EXPERIMENT=${1:-service-outage}
NAMESPACE=${NAMESPACE:-platform-core}
# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  CHAOS EXPERIMENT: $EXPERIMENT"
echo "  Namespace: $NAMESPACE"
echo "  Time: $(date)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
# Baseline SLO Check
echo "┌─ Baseline SLO Check"
echo "│"
# Check current error rate
ERROR_RATE=$(curl -s 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))/sum(rate(http_server_requests_seconds_count[5m]))' | \
  jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
echo -e "│  Error Rate: ${ERROR_RATE}%"
if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
  echo -e "│  ${RED}⚠ Error rate elevated before experiment!${NC}"
  echo -e "│  ${RED}⚠ Aborting - fix baseline first${NC}"
  exit 1
fi
echo -e "│  ${GREEN}✓ Baseline healthy${NC}"
echo "└─"
echo ""
# Run Experiment
case $EXPERIMENT in
  service-outage)
    echo "┌─ Experiment 1: Service Outage"
    echo "│"
    echo "│  Hypothesis: Order service failure triggers circuit breaker"
    echo "│  Expected: Other services continue operating"
    echo "│"
    # Inject 100% error rate
    echo "│  Step 1: Injecting 100% error rate on order-service..."
    curl -X POST http://localhost:8092/api/chaos/error \
      -H "Content-Type: application/json" \
      -d '{
        "service": "order-service",
        "errorRate": 1.0,
        "errorType": "HTTP_503",
        "durationSeconds": 300
      }' -s > /dev/null
    echo -e "│  ${YELLOW}⏳ Waiting for alerts (2 minutes)...${NC}"
    sleep 120
    # Check alerts fired
    echo "│  Step 2: Checking alerts..."
    ALERT_COUNT=$(curl -s 'http://localhost:9090/api/v1/alerts' | jq '[.data.alerts[] | select(.state=="firing")] | length')
    if [ "$ALERT_COUNT" -gt 0 ]; then
      echo -e "│  ${GREEN}✓ Alerts fired: $ALERT_COUNT active${NC}"
    else
      echo -e "│  ${RED}✗ No alerts fired!${NC}"
    fi
    # Check circuit breaker
    echo "│  Step 3: Checking circuit breaker status..."
    CB_STATUS=$(kubectl logs -n istio-system -l app=istiod --tail=50 | grep -c "outlier_detection" || echo "0")
    if [ "$CB_STATUS" -gt 0 ]; then
      echo -e "│  ${GREEN}✓ Circuit breaker engaged${NC}"
    else
      echo -e "│  ${YELLOW}⚠ Circuit breaker status unclear${NC}"
    fi
    # Check other services
    echo "│  Step 4: Checking other services..."
    CATALOG_HEALTH=$(curl -s http://catalog-service.platform-core:8080/actuator/health | jq -r '.status' || echo "DOWN")
    CART_HEALTH=$(curl -s http://cart-service.platform-core:8083/actuator/health | jq -r '.status' || echo "DOWN")
    if [ "$CATALOG_HEALTH" = "UP" ] && [ "$CART_HEALTH" = "UP" ]; then
      echo -e "│  ${GREEN}✓ Other services remain healthy${NC}"
    else
      echo -e "│  ${RED}✗ Other services affected!${NC}"
    fi
    # Rollback
    echo "│  Step 5: Rolling back..."
    curl -X DELETE http://localhost:8092/api/chaos/scenarios/order-service -s > /dev/null
    echo -e "│  ${YELLOW}⏳ Waiting for recovery (1 minute)...${NC}"
    sleep 60
    # Verify recovery
    ORDER_HEALTH=$(curl -s http://order-service.platform-core:8084/actuator/health | jq -r '.status' || echo "DOWN")
    if [ "$ORDER_HEALTH" = "UP" ]; then
      echo -e "│  ${GREEN}✓ Service recovered${NC}"
    else
      echo -e "│  ${RED}✗ Service still down!${NC}"
    fi
    echo "└─"
    ;;
  latency-injection)
    echo "┌─ Experiment 2: Latency Injection"
    echo "│"
    echo "│  Hypothesis: Timeout policies prevent cascading delays"
    echo "│  Expected: Requests fail fast (timeout < latency)"
    echo "│"
    # Inject 5-second latency
    echo "│  Step 1: Injecting 5-second latency on payment-service..."
    curl -X POST http://localhost:8092/api/chaos/latency \
      -H "Content-Type: application/json" \
      -d '{
        "service": "payment-service",
        "delayMs": 5000,
        "probability": 1.0
      }' -s > /dev/null
    # Test timeout
    echo "│  Step 2: Testing timeout behavior..."
    START=$(date +%s)
    curl -X POST http://localhost:8090/api/orders \
      -H "Content-Type: application/json" \
      -d '{"items": [{"productId": "TEST", "quantity": 1}]}' \
      -s -o /dev/null -w "%{http_code}" > /tmp/chaos_response.txt
    END=$(date +%s)
    DURATION=$((END - START))
    if [ "$DURATION" -lt 3 ]; then
      echo -e "│  ${GREEN}✓ Request timed out quickly: ${DURATION}s${NC}"
    else
      echo -e "│  ${RED}✗ Timeout took too long: ${DURATION}s${NC}"
    fi
    # Cleanup
    echo "│  Step 3: Cleaning up..."
    curl -X DELETE http://localhost:8092/api/chaos/scenarios/payment-service -s > /dev/null
    echo "└─"
    ;;
  resource-exhaustion)
    echo "┌─ Experiment 3: Resource Exhaustion"
    echo "│"
    echo "│  Hypothesis: HPA scales pods automatically under load"
    echo "│  Expected: CPU saturation triggers autoscaling"
    echo "│"
    # Get initial replica count
    INITIAL_REPLICAS=$(kubectl get deployment catalog-service -n $NAMESPACE -o jsonpath='{.spec.replicas}')
    echo "│  Initial replicas: $INITIAL_REPLICAS"
    # Generate load
    echo "│  Step 1: Generating high load (5 minutes)..."
    kubectl run load-generator --image=williamyeh/hey:latest --rm -it --restart=Never -- \
      -z 5m -c 50 -q 100 \
      http://catalog-service.$NAMESPACE:8080/api/products &
    LOAD_PID=$!
    echo -e "│  ${YELLOW}⏳ Monitoring autoscaling...${NC}"
    for i in {1..10}; do
      sleep 30
      CURRENT_REPLICAS=$(kubectl get deployment catalog-service -n $NAMESPACE -o jsonpath='{.spec.replicas}')
      echo "│  [$i/10] Current replicas: $CURRENT_REPLICAS"
      if [ "$CURRENT_REPLICAS" -gt "$INITIAL_REPLICAS" ]; then
        echo -e "│  ${GREEN}✓ HPA scaled up!${NC}"
        break
      fi
    done
    # Wait for load test to complete
    wait $LOAD_PID || true
    echo -e "│  ${YELLOW}⏳ Waiting for scale down (5 minutes)...${NC}"
    sleep 300
    FINAL_REPLICAS=$(kubectl get deployment catalog-service -n $NAMESPACE -o jsonpath='{.spec.replicas}')
    if [ "$FINAL_REPLICAS" -eq "$INITIAL_REPLICAS" ]; then
      echo -e "│  ${GREEN}✓ HPA scaled back down${NC}"
    else
      echo -e "│  ${YELLOW}⚠ Still at $FINAL_REPLICAS replicas${NC}"
    fi
    echo "└─"
    ;;
  *)
    echo "Unknown experiment: $EXPERIMENT"
    echo "Available experiments:"
    echo "  - service-outage"
    echo "  - latency-injection"
    echo "  - resource-exhaustion"
    exit 1
    ;;
esac
# Final SLO Check
echo ""
echo "┌─ Final SLO Check"
echo "│"
ERROR_RATE_FINAL=$(curl -s 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))/sum(rate(http_server_requests_seconds_count[5m]))' | \
  jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
echo "│  Final Error Rate: ${ERROR_RATE_FINAL}%"
if (( $(echo "$ERROR_RATE_FINAL < 0.01" | bc -l) )); then
  echo -e "│  ${GREEN}✓ System recovered to baseline${NC}"
  echo "└─"
  echo ""
  echo -e "${GREEN}✅ Experiment PASSED${NC}"
  exit 0
else
  echo -e "│  ${RED}✗ Error rate still elevated!${NC}"
  echo "└─"
  echo ""
  echo -e "${RED}❌ Experiment FAILED - manual intervention required${NC}"
  exit 1
fi
