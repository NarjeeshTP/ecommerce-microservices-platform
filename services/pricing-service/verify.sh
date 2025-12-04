#!/bin/bash

# Pricing Service - Verification Script
# This script verifies that all fixes are working correctly

set -e

echo "=================================="
echo "Pricing Service - Verification"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

function test_step() {
    echo -e "${YELLOW}Testing:${NC} $1"
}

function test_pass() {
    echo -e "${GREEN}✅ PASS:${NC} $1"
    ((PASSED++))
}

function test_fail() {
    echo -e "${RED}❌ FAIL:${NC} $1"
    ((FAILED++))
}

# 1. Check if service directory exists
test_step "Service directory exists"
if [ -d "/Users/narjeeshabdulkhadar/ecommerce-microservices-platform/services/pricing-service" ]; then
    test_pass "Service directory found"
else
    test_fail "Service directory not found"
    exit 1
fi

cd /Users/narjeeshabdulkhadar/ecommerce-microservices-platform/services/pricing-service

# 2. Check if pom.xml exists
test_step "Maven configuration exists"
if [ -f "pom.xml" ]; then
    test_pass "pom.xml found"
else
    test_fail "pom.xml not found"
    exit 1
fi

# 3. Check Java version
test_step "Java 17 is installed"
if java -version 2>&1 | grep -q "17"; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    test_pass "Java 17 detected: $JAVA_VERSION"
else
    test_fail "Java 17 not detected"
fi

# 4. Check Maven is installed
test_step "Maven is installed"
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    test_pass "Maven detected: $MVN_VERSION"
else
    test_fail "Maven not found"
    exit 1
fi

# 5. Compile the service
test_step "Compiling service"
if mvn clean compile -q -DskipTests > /dev/null 2>&1; then
    test_pass "Service compiles successfully"
else
    test_fail "Compilation failed"
fi

# 6. Run tests
test_step "Running tests (this may take a minute...)"
if mvn test -q > /tmp/pricing-test-output.txt 2>&1; then
    TEST_RESULT=$(grep "Tests run:" /tmp/pricing-test-output.txt | tail -1)
    test_pass "All tests passed: $TEST_RESULT"
else
    TEST_RESULT=$(grep "Tests run:" /tmp/pricing-test-output.txt | tail -1 || echo "Unknown")
    test_fail "Some tests failed: $TEST_RESULT"
fi

# 7. Check docker-compose.yml
test_step "Docker compose file is valid"
cd ../../infra
if docker compose config > /dev/null 2>&1; then
    test_pass "docker-compose.yml is valid"
else
    test_fail "docker-compose.yml has errors"
fi

# 8. Check if documentation exists
test_step "Documentation files exist"
cd ../services/pricing-service
if [ -f "README.md" ] && [ -f "PHASE3_COMPLETION_SUMMARY.md" ] && [ -f "QUICK_START.md" ]; then
    test_pass "All documentation files present"
else
    test_fail "Some documentation files missing"
fi

# 9. Check README.md completeness
test_step "README.md is comprehensive"
README_LINES=$(wc -l < README.md)
if [ "$README_LINES" -gt 500 ]; then
    test_pass "README.md is comprehensive ($README_LINES lines)"
else
    test_fail "README.md seems incomplete ($README_LINES lines)"
fi

# 10. Check if Docker is running
test_step "Docker is running"
if docker info > /dev/null 2>&1; then
    test_pass "Docker daemon is running"
else
    test_fail "Docker daemon is not running"
fi

# 11. Check if key source files exist
test_step "Key source files exist"
MISSING_FILES=0
FILES_TO_CHECK=(
    "src/main/java/com/ecommerce/pricingservice/PricingServiceApplication.java"
    "src/main/java/com/ecommerce/pricingservice/controller/PricingController.java"
    "src/main/java/com/ecommerce/pricingservice/service/PricingService.java"
    "src/main/java/com/ecommerce/pricingservice/repository/PricingRuleRepository.java"
    "src/main/java/com/ecommerce/pricingservice/config/RedisConfig.java"
    "src/main/resources/application.yml"
    "src/main/resources/db/migration/V1__Create_pricing_rules_table.sql"
)

for file in "${FILES_TO_CHECK[@]}"; do
    if [ ! -f "$file" ]; then
        ((MISSING_FILES++))
    fi
done

if [ "$MISSING_FILES" -eq 0 ]; then
    test_pass "All key source files present"
else
    test_fail "$MISSING_FILES source files are missing"
fi

# 12. Check if cache eviction is fixed
test_step "Cache eviction fix is present"
if grep -q "@CacheEvict(value = \"prices\", allEntries = true)" src/main/java/com/ecommerce/pricingservice/service/PricingService.java; then
    test_pass "Cache eviction fix confirmed (allEntries = true)"
else
    test_fail "Cache eviction fix not found"
fi

echo ""
echo "=================================="
echo "Verification Summary"
echo "=================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "=================================="

if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}🎉 All checks passed! Service is ready.${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Some checks failed. Please review.${NC}"
    exit 1
fi

