# Scripts Directory

This directory contains all utility scripts for the Pricing Service.

## 📁 Directory Structure

```
scripts/
├── load-tests/           # Performance and load testing scripts
│   ├── k6-load-test.js              # Main k6 load test script
│   ├── run-benchmark.sh             # Standard benchmark (100 users)
│   ├── run-quick-test.sh            # Quick test (configurable users)
│   ├── run-heavy-load-test.sh       # Heavy load test (500+ users)
│   └── run-stress-test.sh           # Stress test (find breaking point)
├── tests/                # Test scripts
│   └── test-circuit-breaker.sh      # Circuit breaker testing
└── verify.sh             # Service verification script
```

---

## 🚀 Load Testing Scripts

### Location
All load test scripts are in: `scripts/load-tests/`

### Usage

#### 1. Standard Benchmark (100 users)
```bash
cd scripts/load-tests
./run-benchmark.sh
```
- **Users**: 10 → 50 → 100
- **Duration**: ~4 minutes
- **Purpose**: Standard validation

#### 2. Quick Test (Configurable)
```bash
cd scripts/load-tests
./run-quick-test.sh [MAX_USERS]

# Examples:
./run-quick-test.sh         # 200 users (default)
./run-quick-test.sh 300     # 300 users
./run-quick-test.sh 500     # 500 users
```
- **Duration**: ~2 minutes
- **Purpose**: Fast validation

#### 3. Heavy Load Test (500+ users)
```bash
cd scripts/load-tests
./run-heavy-load-test.sh

# Customize:
MAX_USERS=1000 ./run-heavy-load-test.sh
```
- **Duration**: ~8 minutes
- **Purpose**: Black Friday simulation

#### 4. Stress Test (Find breaking point)
```bash
cd scripts/load-tests
./run-stress-test.sh

# Customize:
START_USERS=100 MAX_USERS=2000 ./run-stress-test.sh
```
- **Duration**: ~20 minutes
- **Purpose**: Find maximum capacity

### Results Location
All test results are saved in the service root directory:
- `../../k6-results.json` - Benchmark results
- `../../k6-quick-results.json` - Quick test results
- `../../k6-heavy-results.json` - Heavy load results
- `../../k6-stress-results.json` - Stress test results

---

## 🧪 Test Scripts

### Location
Test scripts are in: `scripts/tests/`

### Circuit Breaker Test
```bash
cd scripts/tests
./test-circuit-breaker.sh
```
- Tests circuit breaker state transitions
- Validates fallback behavior
- Shows metrics and events

---

## ✅ Verification Script

### Location
Root of scripts directory: `scripts/verify.sh`

### Usage
```bash
cd scripts
./verify.sh
```
- Verifies service health
- Checks dependencies
- Validates configuration

---

## 🔧 Environment Variables

All scripts support these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | http://localhost:8083 | Service URL |
| `MAX_USERS` | Varies | Maximum concurrent users |
| `NORMAL_USERS` | MAX_USERS/2 | Normal load users |
| `RAMP_DURATION` | Varies | Ramp-up time |
| `NORMAL_DURATION` | Varies | Normal load duration |
| `SPIKE_DURATION` | Varies | Spike duration |
| `PEAK_DURATION` | Varies | Peak load duration |

---

## 📝 Running Scripts from Root

You can also run scripts from the service root directory:

```bash
# From: /services/pricing-service/

# Load tests
./scripts/load-tests/run-benchmark.sh
./scripts/load-tests/run-quick-test.sh 200
./scripts/load-tests/run-heavy-load-test.sh
./scripts/load-tests/run-stress-test.sh

# Circuit breaker test
./scripts/tests/test-circuit-breaker.sh

# Verification
./scripts/verify.sh
```

---

## 📚 Documentation

For detailed information, see:
- Main `README.md` - Complete service documentation (load testing, circuit breaker, etc.)
- `K6-BENCHMARK-RESULTS.md` - Benchmark test results
- `K6-200USERS-RESULTS.md` - 200 users test results

---

## 🎯 Quick Reference

### Most Common Commands

```bash
# Quick performance check (200 users)
cd scripts/load-tests && ./run-quick-test.sh

# Full benchmark (100 users)
cd scripts/load-tests && ./run-benchmark.sh

# Test circuit breaker
cd scripts/tests && ./test-circuit-breaker.sh

# Verify service
cd scripts && ./verify.sh
```

---

**All scripts are executable and ready to use!** 🚀

