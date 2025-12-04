#!/bin/bash

# Pricing Service Performance Test Runner
# This script runs k6 performance tests against the pricing service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Pricing Service Performance Tests ===${NC}\n"

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8083}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}Error: k6 is not installed${NC}"
    echo "Please install k6 from https://k6.io/docs/get-started/installation/"
    echo ""
    echo "Quick install options:"
    echo "  macOS:   brew install k6"
    echo "  Linux:   See README.md for apt installation"
    echo "  Windows: choco install k6"
    exit 1
fi

# Check if service is running
echo -e "${YELLOW}Checking if pricing service is running at ${BASE_URL}...${NC}"
if ! curl -s -f "${BASE_URL}/actuator/health" > /dev/null; then
    echo -e "${RED}Error: Pricing service is not responding at ${BASE_URL}${NC}"
    echo "Please start the service first:"
    echo "  cd ../.. && mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✓ Service is running${NC}\n"

# Determine which test to run
TEST_TYPE="${1:-smoke}"

case $TEST_TYPE in
    smoke)
        echo -e "${YELLOW}Running smoke test...${NC}"
        k6 run --env BASE_URL="${BASE_URL}" "${SCRIPT_DIR}/smoke-test.k6.js"
        ;;
    load)
        echo -e "${YELLOW}Running load test...${NC}"
        k6 run --env BASE_URL="${BASE_URL}" "${SCRIPT_DIR}/pricing-load-test.k6.js"
        ;;
    all)
        echo -e "${YELLOW}Running all tests...${NC}"
        echo -e "\n${YELLOW}1/2: Smoke test${NC}"
        k6 run --env BASE_URL="${BASE_URL}" "${SCRIPT_DIR}/smoke-test.k6.js"
        echo -e "\n${YELLOW}2/2: Load test${NC}"
        k6 run --env BASE_URL="${BASE_URL}" "${SCRIPT_DIR}/pricing-load-test.k6.js"
        ;;
    *)
        echo -e "${RED}Invalid test type: ${TEST_TYPE}${NC}"
        echo "Usage: $0 [smoke|load|all]"
        echo "  smoke - Quick smoke test (default)"
        echo "  load  - Comprehensive load test"
        echo "  all   - Run all tests"
        exit 1
        ;;
esac

echo -e "\n${GREEN}=== Performance tests completed ===${NC}"
