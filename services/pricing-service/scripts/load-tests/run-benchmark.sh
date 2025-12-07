#!/bin/bash

# Performance benchmark script for Pricing Service
# This script runs k6 load tests against the Pricing Service

set -e

echo "======================================"
echo "Pricing Service Performance Benchmark"
echo "======================================"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "❌ k6 is not installed."
    echo "📦 Install k6:"
    echo "   macOS: brew install k6"
    echo "   Linux: sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69"
    echo "          echo 'deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main' | sudo tee /etc/apt/sources.list.d/k6.list"
    echo "          sudo apt-get update && sudo apt-get install k6"
    echo "   Windows: choco install k6"
    exit 1
fi

# Default values
BASE_URL="${BASE_URL:-http://localhost:8083}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TEST_FILE="$SCRIPT_DIR/k6-load-test.js"

echo "🎯 Target: $BASE_URL"
echo "📄 Test file: k6-load-test.js"
echo ""

# Check if service is running
echo "🔍 Checking if Pricing Service is running..."
if curl -sf "$BASE_URL/actuator/health" > /dev/null; then
    echo "✅ Pricing Service is healthy"
else
    echo "❌ Pricing Service is not reachable at $BASE_URL"
    echo "   Start the service with: mvn spring-boot:run"
    exit 1
fi

echo ""
echo "🚀 Starting k6 load test..."
echo "   This will take approximately 4 minutes"
echo ""

# Run k6 test
k6 run \
  --out json=../../k6-results.json \
  -e BASE_URL="$BASE_URL" \
  "$TEST_FILE"

echo ""
echo "======================================"
echo "✅ Benchmark Complete!"
echo "======================================"
echo ""
echo "📊 Results saved to: k6-results.json"
echo ""
echo "📈 To visualize results:"
echo "   1. Install k6 cloud (optional): k6 login cloud"
echo "   2. Or use Grafana k6 dashboard"
echo "   3. Or analyze k6-results.json manually"
echo ""


