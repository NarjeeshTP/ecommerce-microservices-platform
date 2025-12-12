#!/bin/bash
set -e
NAMESPACE=${1:-platform-core-staging}
echo "🧪 Running smoke tests for namespace: $NAMESPACE"
# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
# Test counter
PASSED=0
FAILED=0
# Helper function
test_endpoint() {
  local service=$1
  local path=$2
  local expected_status=${3:-200}
  echo -n "Testing $service$path... "
  if kubectl run curl-test-$$-$RANDOM --rm -it --restart=Never --image=curlimages/curl -n $NAMESPACE -- \
    curl -s -o /dev/null -w "%{http_code}" http://$service.$NAMESPACE:8080$path | grep -q $expected_status; then
    echo -e "${GREEN}✓ PASSED${NC}"
    ((PASSED++))
  else
    echo -e "${RED}✗ FAILED${NC}"
    ((FAILED++))
  fi
}
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SMOKE TEST SUITE"
echo "  Namespace: $NAMESPACE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
# Test 1: Check pods are running
echo "┌─ Test 1: Pod Status"
echo "│"
RUNNING_PODS=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l)
TOTAL_PODS=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ "$RUNNING_PODS" -eq "$TOTAL_PODS" ] && [ "$TOTAL_PODS" -gt 0 ]; then
  echo -e "│  ${GREEN}✓${NC} All $TOTAL_PODS pods are running"
  ((PASSED++))
else
  echo -e "│  ${RED}✗${NC} Only $RUNNING_PODS/$TOTAL_PODS pods are running"
  ((FAILED++))
  kubectl get pods -n $NAMESPACE
fi
echo "└─"
echo ""
# Test 2: Health check endpoints
echo "┌─ Test 2: Health Endpoints"
echo "│"
for service in catalog-service pricing-service cart-service order-service; do
  if kubectl get svc $service -n $NAMESPACE &>/dev/null; then
    test_endpoint $service "/actuator/health"
  else
    echo "│  ⊘ $service not deployed, skipping..."
  fi
done
echo "└─"
echo ""
# Test 3: API endpoints
echo "┌─ Test 3: API Endpoints"
echo "│"
test_endpoint "catalog-service" "/api/products" "200"
test_endpoint "pricing-service" "/api/pricing/health" "200"
test_endpoint "cart-service" "/api/cart/health" "200"
echo "└─"
echo ""
# Test 4: Liveness probes
echo "┌─ Test 4: Liveness Probes"
echo "│"
for service in catalog-service pricing-service cart-service; do
  if kubectl get svc $service -n $NAMESPACE &>/dev/null; then
    test_endpoint $service "/actuator/health/liveness"
  fi
done
echo "└─"
echo ""
# Test 5: Readiness probes
echo "┌─ Test 5: Readiness Probes"
echo "│"
for service in catalog-service pricing-service cart-service; do
  if kubectl get svc $service -n $NAMESPACE &>/dev/null; then
    test_endpoint $service "/actuator/health/readiness"
  fi
done
echo "└─"
echo ""
# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  TEST SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "  Passed: ${GREEN}$PASSED${NC}"
echo -e "  Failed: ${RED}$FAILED${NC}"
echo ""
if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}✅ All smoke tests passed!${NC}"
  exit 0
else
  echo -e "${RED}❌ Some smoke tests failed!${NC}"
  exit 1
fi
