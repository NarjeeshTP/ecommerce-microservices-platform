# CI/CD Pipeline Documentation

> **Quick Start?** See [QUICKSTART.md](QUICKSTART.md) for a fast setup checklist.

This directory contains GitHub Actions workflows for the E-Commerce Microservices Platform with automated testing, coverage reporting, Docker builds, and security scanning.

## 📊 Overview

**Status:** ✅ Production-ready CI/CD pipeline configured

**Features:**
- 🧪 Automated testing (34 tests: 15 unit + 7 integration + 12 service)
- 📈 Code coverage tracking (70% minimum threshold with JaCoCo)
- 🐳 Docker automation (build & push to ghcr.io)
- 🔒 Security scanning (Trivy vulnerability detection)
- ⚡ Smart path filtering (only test changed services)
- 🚀 Fast execution (~3 minutes total)

## 📁 File Structure

```
.github/
├── workflows/
│   ├── catalog-service-ci.yml    # Catalog service pipeline (220 lines)
│   ├── ci.yml                     # Monorepo-wide pipeline (140 lines)
│   └── qodana_code_quality.yml   # Code quality analysis
├── README.md                      # This file - Complete documentation
├── QUICKSTART.md                  # Fast setup guide & checklist
└── markdown-link-check-config.json # Doc validation config
```

## Workflows

### 1. `catalog-service-ci.yml` - Catalog Service CI Pipeline

**Triggers:**
- Push to `main`, `develop`, or `staging` branches
- Pull requests to these branches
- Only runs when files in `services/catalog-service/` or `platform-libraries/` change

**Jobs:**

#### Test Job
- **Unit Tests**: Fast isolated tests with mocked dependencies
- **Integration Tests**: Full E2E tests with Testcontainers (PostgreSQL)
- **Coverage Report**: JaCoCo code coverage with 70% minimum threshold
- **Test Reporting**: Automatic test result publishing and PR comments

#### Build Job
- Builds the JAR file (after tests pass)
- Uploads artifact for deployment
- Caches Maven dependencies for faster builds

#### Docker Job
- Builds Docker image (only on `main` or `develop` push)
- Pushes to GitHub Container Registry (ghcr.io)
- Tags: `latest`, `branch-name`, `branch-sha`

#### Security Scan Job
- Runs Trivy vulnerability scanner
- Uploads results to GitHub Security tab

**Requirements:**
- Java 17
- Maven
- Docker (for Testcontainers)
- GitHub token (automatic)

**Artifacts:**
- Test results (XML/HTML reports)
- JaCoCo coverage reports
- JAR file
- Docker image (on ghcr.io)

---

### 2. `ci.yml` - Monorepo CI Pipeline

**Triggers:**
- Push/PR to main branches
- Runs for all services

**Features:**
- **Path Filtering**: Only tests changed services
- **Parallel Execution**: Services tested concurrently
- **Lint & Quality**: Code quality checks
- **Documentation Validation**: OpenAPI spec and Markdown link checks

**Jobs:**
- `detect-changes`: Identifies which services changed
- `test-catalog`: Tests catalog service (conditional)
- `test-pricing`: Tests pricing service (conditional, future)
- `lint`: Code quality checks
- `docs`: Documentation validation
- `summary`: Overall CI status

---

## 🔄 Pipeline Flow

```
Developer Push/PR → Path Filter → Test Jobs (parallel) → Build → Docker (main/develop) → Security Scan
                         ↓
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
   Unit Tests      Integration      Service Tests
   (~1 second)     (~15 seconds)    (~300ms)
        ↓                ↓                ↓
        └────────────────┼────────────────┘
                         ↓
                  Coverage Check (>70%)
                         ↓
                   All Pass? ✅/❌
```

**Pipeline Timing:**
```
Unit Tests:           ~1 second
Integration Tests:    ~15 seconds (with Testcontainers)
Service Tests:        ~300ms
Coverage Report:      ~2 seconds
Build JAR:           ~20 seconds
Docker Build:        ~60 seconds (main/develop only)
Security Scan:       ~30 seconds
────────────────────────────────
Total (PR):          ~1.5 minutes
Total (main):        ~3 minutes
```

**With Maven Cache (2nd+ run):**
- First run: ~5 minutes (downloading dependencies)
- Cached runs: ~3 minutes (40% faster)

---

## Setup Instructions

### 1. Enable GitHub Actions

GitHub Actions is enabled by default on GitHub repositories. The workflows will run automatically on push/PR.

### 2. Configure Secrets (Optional)

For private Docker registries or external services, add secrets in:
```
Settings > Secrets and variables > Actions
```

**Currently Required Secrets:**
- `GITHUB_TOKEN` - Automatically provided by GitHub

**Future Secrets (for production):**
- `DOCKER_REGISTRY_TOKEN` - If using private registry
- `SONAR_TOKEN` - If using SonarQube
- `SLACK_WEBHOOK` - For notifications

### 3. Branch Protection Rules (Recommended)

Set up branch protection in `Settings > Branches`:

**For `main` branch:**
- ✅ Require pull request reviews (1 reviewer)
- ✅ Require status checks to pass before merging
  - Select: "Test Catalog Service", "Lint & Code Quality"
- ✅ Require branches to be up to date before merging
- ✅ Require conversation resolution before merging

**For `develop` branch:**
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging

### 4. Enable GitHub Container Registry

Docker images are pushed to `ghcr.io`. To pull images:

```bash
# Login to GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Pull image
docker pull ghcr.io/YOUR_USERNAME/ecommerce-microservices-platform/catalog-service:latest
```

---

## Running Tests Locally (Before Push)

### Unit Tests Only
```bash
cd services/catalog-service
mvn test -Dtest=CatalogControllerTest,ItemServiceTest
```

### Integration Tests Only
```bash
cd services/catalog-service
mvn test -Dtest=CatalogControllerIntegrationTest
```

### All Tests with Coverage
```bash
cd services/catalog-service
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Lint (if configured)
```bash
cd services/catalog-service
mvn checkstyle:check
mvn spotbugs:check
```

---

## CI Performance Optimization

### 1. Maven Dependency Caching
The workflows cache `~/.m2/repository` to speed up builds.

**Cache Key:** Based on `pom.xml` hash
**Cache Restoration:** Reuses cache if pom.xml unchanged

### 2. Path Filtering
Only affected services are tested on changes.

**Example:** Changing `catalog-service/` won't trigger `pricing-service` tests.

### 3. Parallel Execution
Multiple services can be tested in parallel.

### 4. Docker Layer Caching
Docker builds use GitHub Actions cache for faster image builds.

---

## Monitoring & Debugging

### View Test Results
1. Go to **Actions** tab in GitHub
2. Click on a workflow run
3. Click on the **Test** job
4. Download **test-results** artifact

### View Coverage Reports
1. Go to **Actions** tab
2. Click on a workflow run
3. Download **test-results** artifact
4. Open `site/jacoco/index.html`

### View Security Scan Results
1. Go to **Security** tab
2. Click on **Code scanning alerts**
3. View Trivy vulnerability reports

### Debug Failed Tests
```bash
# View logs for a specific test
cd services/catalog-service
mvn test -Dtest=FailingTestClass -X

# Run integration tests with verbose output
mvn test -Dtest=CatalogControllerIntegrationTest -X
```

---

## CI/CD Best Practices

### ✅ DO
- Run tests locally before pushing
- Keep tests fast (unit tests < 1s, integration < 30s)
- Write meaningful test names
- Use Testcontainers for integration tests
- Keep dependencies up to date
- Add code coverage for new features
- Use semantic commit messages

### ❌ DON'T
- Push directly to `main` (use PRs)
- Skip tests locally
- Commit with failing tests
- Ignore security warnings
- Leave TODO comments without tracking
- Merge PRs with failing CI

---

## Troubleshooting

### Issue: Tests pass locally but fail in CI

**Possible Causes:**
- Different Java version (CI uses Java 17)
- Missing environment variables
- Time-zone differences
- Docker not available (for Testcontainers)

**Solution:**
```bash
# Run with CI-like environment
docker run -it --rm \
  -v "$PWD":/workspace \
  -w /workspace/services/catalog-service \
  eclipse-temurin:17-jdk \
  mvn clean test
```

### Issue: Testcontainers fail in CI

**Error:** "Could not start container"

**Solution:** Already handled - CI uses Docker-in-Docker support.

### Issue: Maven dependency download is slow

**Solution:** Cache is configured. First run will be slow, subsequent runs will be fast.

### Issue: Coverage threshold not met

**Error:** "Coverage 65% is below minimum 70%"

**Solution:** Add more tests to increase coverage:
```bash
# Generate coverage report
mvn test jacoco:report

# Open report and see untested code
open target/site/jacoco/index.html
```

---

## ✅ What's Configured

### GitHub Actions Workflows

#### 1. `catalog-service-ci.yml` - Dedicated Catalog Pipeline
**Jobs:**
- **Test** - Runs all 34 tests (unit + integration + service) with Testcontainers
- **Build** - Creates JAR artifact
- **Docker** - Builds and pushes image to ghcr.io (main/develop only)
- **Security** - Trivy vulnerability scanning

**Triggers:** Push/PR to main/develop/staging (only when catalog-service changes)

#### 2. `ci.yml` - Monorepo Pipeline
**Jobs:**
- **detect-changes** - Smart path filtering for multiple services
- **test-catalog** - Conditional catalog testing
- **lint** - Code quality checks (Checkstyle, SpotBugs)
- **docs** - OpenAPI and Markdown validation
- **summary** - Overall CI status

**Ready for scaling:** pricing, cart, order, payment, inventory services

### JaCoCo Code Coverage
- Minimum threshold: 70%
- HTML reports: `target/site/jacoco/index.html`
- XML reports for CI integration
- Automatic PR comments with coverage

### Build Configuration
- Java 17 with Temurin distribution
- Maven dependency caching
- Docker layer caching
- Testcontainers for integration tests
- PostgreSQL 15-alpine for test database

---

## Adding CI for New Services

To add CI for a new service (e.g., `pricing-service`):

1. **Update `ci.yml`** path filters:
```yaml
pricing:
  - 'services/pricing-service/**'
  - 'platform-libraries/**'
```

2. **Add test job**:
```yaml
test-pricing:
  name: Test Pricing Service
  needs: detect-changes
  if: needs.detect-changes.outputs.pricing == 'true'
  # ... copy test-catalog job structure
```

3. **Create dedicated workflow** (optional):
```yaml
# .github/workflows/pricing-service-ci.yml
```

---

## Metrics & SLOs

**CI Pipeline SLOs:**
- **Build Time**: < 5 minutes for unit tests
- **Test Coverage**: > 70% for all services
- **Success Rate**: > 95% (excluding flaky tests)
- **Docker Build**: < 3 minutes

**Current Performance:**
- Unit Tests: ~1 second
- Integration Tests: ~10-15 seconds
- Full Pipeline: ~3-4 minutes

---

## Future Enhancements

- [ ] Add contract testing (Pact)
- [ ] Add performance testing (k6)
- [ ] Add E2E tests with all services
- [ ] Add SonarQube integration
- [ ] Add automated dependency updates (Dependabot)
- [ ] Add automated release notes generation
- [ ] Add deployment to staging/production
- [ ] Add smoke tests post-deployment
- [ ] Add load testing in CI
- [ ] Add mutation testing (PIT)

---

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)

