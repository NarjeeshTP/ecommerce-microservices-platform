# 🚀 CI/CD Quick Start Guide

Complete this checklist to get your CI/CD pipeline running in minutes!

## ⚡ Fast Track (5 Minutes)

```bash
# 1. Verify tests pass locally
cd services/catalog-service && mvn clean test

# 2. Check coverage (optional)
mvn test jacoco:report && open target/site/jacoco/index.html

# 3. Commit and push
cd ../..
git add .github/ services/catalog-service/pom.xml
git commit -m "feat: add CI/CD pipeline with automated tests"
git push origin main

# 4. Watch it run
# Go to GitHub → Actions tab → See workflows execute
```

**That's it!** Your pipeline is now running automatically. ✅

---

## 📋 Pre-Push Checklist

### Local Verification
- [ ] **Tests pass locally**
  ```bash
  cd services/catalog-service
  mvn clean test
  ```
  Expected: All 34 tests pass in ~20 seconds

- [ ] **Coverage meets threshold** (>70%)
  ```bash
  mvn test jacoco:report
  open target/site/jacoco/index.html
  ```

- [ ] **JAR builds successfully**
  ```bash
  mvn clean package -DskipTests
  ```

- [ ] **No compilation errors**
  ```bash
  mvn clean compile
  ```

### Code Quality
- [ ] Code follows Java conventions
- [ ] Tests have meaningful names
- [ ] No commented-out code
- [ ] No secrets/credentials in code
- [ ] Commit messages are descriptive

---

## 🔧 GitHub Setup (After First Push)

### 1. Verify First Run
- [ ] Go to **GitHub repository** → **Actions** tab
- [ ] See workflows appear automatically
- [ ] Click on running workflow
- [ ] Verify jobs complete:
  - ✅ Test Job (~30 seconds)
  - ✅ Build Job (~30 seconds)
  - ✅ Docker Job (~60 seconds, main/develop only)
  - ✅ Security Scan (~30 seconds)

### 2. Configure Branch Protection (Recommended)
- [ ] Go to **Settings** → **Branches** → **Add rule**
- [ ] Branch name pattern: `main`
- [ ] Check these options:
  - ✅ Require pull request reviews (1 reviewer)
  - ✅ Require status checks to pass before merging
  - ✅ Require branches to be up to date
  - ✅ Require conversation resolution
- [ ] Select required status checks:
  - `Unit & Integration Tests`
  - `Build & Package`
- [ ] Click **Create** or **Save changes**

### 3. Enable GitHub Container Registry (For Docker)
- [ ] Go to **Settings** → **Actions** → **General**
- [ ] Under **Workflow permissions**:
  - ✅ Select: **Read and write permissions**
  - ✅ Check: **Allow GitHub Actions to create and approve pull requests**
- [ ] Click **Save**

---

## 🧪 Test the Pipeline

### Test 1: Create a Test PR
```bash
# Create a test branch
git checkout -b test-ci-pipeline

# Make a small change
echo "# CI Test" >> services/catalog-service/README.md

# Commit and push
git add .
git commit -m "test: trigger CI pipeline"
git push origin test-ci-pipeline

# Create PR on GitHub
# Watch CI run automatically!
```

**Expected Result:**
- ✅ CI runs in ~1.5 minutes
- ✅ All tests pass
- ✅ Coverage report posted to PR
- ✅ Green checkmark appears

### Test 2: Verify Coverage Threshold
```bash
# Check current coverage
cd services/catalog-service
mvn test jacoco:report
open target/site/jacoco/index.html

# Should be > 70%
```

### Test 3: Verify Docker Build (main/develop only)
```bash
# Merge PR to main
git checkout main
git merge test-ci-pipeline
git push origin main

# Check GitHub Actions
# Docker job should run and push to ghcr.io
```

**Expected Result:**
- ✅ Docker image built
- ✅ Pushed to ghcr.io
- ✅ Visible in repository **Packages**

---

## 📊 Monitoring & Validation

### View Test Results
1. **GitHub Actions** → Select workflow run
2. Click **Test** job
3. Download **test-results** artifact
4. Extract and view `surefire-reports/`

### View Coverage Report
1. Download **test-results** artifact
2. Extract and open `site/jacoco/index.html`
3. Verify >70% coverage
4. Identify uncovered lines (red)

### View Docker Images
1. Go to repository main page
2. Click **Packages** (right sidebar)
3. See `catalog-service` package
4. Check tags: `latest`, `main-<sha>`, `main`

### View Security Scan
1. Go to **Security** tab
2. Click **Code scanning**
3. Review Trivy scan results
4. Fix critical vulnerabilities if any

---

## 🐛 Common Issues & Quick Fixes

### ❌ Issue: "Tests fail in CI but pass locally"

**Quick Fix:**
```bash
# Run tests in Docker (CI environment)
docker run -it --rm \
  -v "$PWD":/workspace \
  -w /workspace/services/catalog-service \
  eclipse-temurin:17-jdk \
  mvn clean test
```

**Check:**
- Java version (CI uses Java 17)
- Environment variables
- Docker availability for Testcontainers

### ❌ Issue: "Coverage below threshold"

**Quick Fix:**
```bash
# Generate coverage report
cd services/catalog-service
mvn test jacoco:report
open target/site/jacoco/index.html

# Identify untested code (red lines)
# Add tests to cover them
# Aim for >75% to have buffer
```

### ❌ Issue: "Docker build fails"

**Check:**
- [ ] Dockerfile exists in `services/catalog-service/`
- [ ] JAR file created successfully
- [ ] GITHUB_TOKEN has write permissions (Settings → Actions)
- [ ] Branch is `main` or `develop` (Docker only runs on these)

### ❌ Issue: "Workflow doesn't trigger"

**Check:**
- [ ] Changed files match path patterns (`services/catalog-service/**`)
- [ ] Branch matches trigger (main/develop/staging)
- [ ] GitHub Actions enabled (Settings → Actions)
- [ ] Workflow file is in `.github/workflows/`

---

## 🎯 Success Criteria

Your CI/CD is working correctly when:

- ✅ Tests run automatically on every PR
- ✅ PR shows status check with test results
- ✅ Coverage report posted as PR comment
- ✅ Failed tests block merge
- ✅ Docker image built on main/develop
- ✅ Security scans report to Security tab
- ✅ Total pipeline time < 5 minutes
- ✅ Second run is 40% faster (cache working)

---

## 📈 Performance Benchmarks

**Expected Timings (with cache):**

| Stage | Time | Notes |
|-------|------|-------|
| Unit Tests | < 2s | Fast, mocked dependencies |
| Integration Tests | < 20s | Testcontainers PostgreSQL |
| Service Tests | < 1s | Business logic |
| Total Test Job | < 30s | Parallel execution |
| Build JAR | < 30s | With Maven cache |
| Docker Build | < 90s | With layer cache |
| Security Scan | < 30s | Trivy scanning |
| **Total Pipeline** | **< 4 min** | **Full pipeline** |

**If slower, check:**
- Maven cache is working (`.m2/repository`)
- Docker layer cache enabled
- No network/internet issues
- Testcontainers images cached

---

## 🔄 Daily Workflow

### Making Changes
```bash
# 1. Create feature branch
git checkout -b feature/my-feature

# 2. Make changes and test locally
cd services/catalog-service
mvn clean test

# 3. Check coverage
mvn test jacoco:report
open target/site/jacoco/index.html

# 4. Commit and push
git add .
git commit -m "feat: add new feature"
git push origin feature/my-feature

# 5. Create PR on GitHub
# CI runs automatically!

# 6. Review results in PR
# - Test results
# - Coverage report
# - Security scan

# 7. Merge when all checks pass ✅
```

### Reviewing PRs
```bash
# Check PR on GitHub
# - View CI status (✅ or ❌)
# - Review test results
# - Check coverage report
# - Review code changes
# - Merge if all good!
```

---

## 📚 Commands Reference

### Local Testing
```bash
# All tests
mvn clean test

# Specific test class
mvn test -Dtest=CatalogControllerTest

# With coverage
mvn test jacoco:report

# Check coverage threshold
mvn jacoco:check

# Verbose output
mvn test -X
```

### Build & Package
```bash
# Build JAR (skip tests)
mvn clean package -DskipTests

# Build JAR (with tests)
mvn clean package

# Run JAR
java -jar target/catalog-service-0.0.1-SNAPSHOT.jar
```

### Docker
```bash
# Build image locally
docker build -t catalog-service:local services/catalog-service/

# Run container
docker run -p 8081:8081 catalog-service:local

# Pull from GHCR
docker pull ghcr.io/YOUR_USERNAME/ecommerce-microservices-platform/catalog-service:latest
```

### Git Workflow
```bash
# Create feature branch
git checkout -b feature/my-feature

# Stage changes
git add .

# Commit with message
git commit -m "feat: add feature"

# Push to remote
git push origin feature/my-feature

# Update from main
git pull origin main --rebase
```

---

## 🎓 Next Steps

### Immediate (Today)
1. ✅ Complete pre-push checklist
2. ✅ Push code to GitHub
3. ✅ Verify first CI run
4. ✅ Configure branch protection

### This Week
1. Add CI badge to README:
   ```markdown
   ![CI](https://github.com/USERNAME/REPO/workflows/Catalog%20Service%20CI/badge.svg)
   ```
2. Set up notifications (Slack/Discord) - optional
3. Document process for team
4. Create runbook for common issues

### Next Week
1. Add contract tests (Pact)
2. Add performance tests (k6)
3. Set up staging deployment
4. Add smoke tests

### This Month
1. Implement blue-green deployments
2. Add canary releases
3. Set up SonarQube
4. Add mutation testing (PIT)

---

## 💡 Pro Tips

1. **Run tests before pushing** - Catch issues early
2. **Use meaningful commit messages** - Makes debugging easier
3. **Keep tests fast** - Unit tests < 1s, integration < 30s
4. **Monitor coverage trends** - Aim for steady increase
5. **Fix flaky tests immediately** - Don't let them accumulate
6. **Review CI logs** - Learn from failures
7. **Cache dependencies** - Already configured, enjoy speed!
8. **Use path filters** - Only test what changed

---

## 📞 Getting Help

**Documentation:**
- Full guide: [README.md](README.md)
- Workflow details: [workflows/catalog-service-ci.yml](workflows/catalog-service-ci.yml)

**Resources:**
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Testcontainers Docs](https://www.testcontainers.org/)
- [JaCoCo Docs](https://www.jacoco.org/)
- [Maven Surefire](https://maven.apache.org/surefire/)

**Troubleshooting:**
- Check GitHub Actions logs
- Review [README.md](README.md) troubleshooting section
- Test locally first
- Compare with working builds

---

## ✅ Completion Checklist

Before considering CI/CD "done":

- [ ] All local tests pass
- [ ] Coverage > 70%
- [ ] First CI run successful
- [ ] Branch protection configured
- [ ] Docker image pushed (if on main/develop)
- [ ] Team notified about new process
- [ ] Documentation reviewed
- [ ] Runbook created for team

---

**Status:** Ready to push! 🚀

**Questions?** See [README.md](README.md) for complete documentation.

