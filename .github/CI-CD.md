# CI/CD & Infrastructure as Code

Complete CI/CD pipelines with GitHub Actions and Terraform infrastructure provisioning for E-Commerce Microservices Platform.

## ✅ Current Status (Dec 12, 2025)

**Version:** 0.1.0  
**Status:** Week 15 Implementation Complete

### Features Implemented
- ✅ **GitHub Actions Workflows** - CI/CD automation
- ✅ **Build-on-Change** - Only build modified services
- ✅ **Container Registry** - Publish to GitHub Container Registry (GHCR)
- ✅ **Helm Deployment** - Automated deployment to staging
- ✅ **Terraform** - Infrastructure as Code scaffolding
- ✅ **SBOM Generation** - Software Bill of Materials
- ✅ **Security Scanning** - SAST and dependency checks
- ✅ **Slack Notifications** - Deployment status alerts

---

## Architecture

### CI/CD Pipeline Flow

```
┌─────────────────────────────────────────────────────┐
│  Developer                                          │
│  ↓                                                  │
│  git push origin feature/new-feature                │
└──────────────────────┬──────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────┐
│  GitHub Actions (CI)                                │
│                                                     │
│  1. Detect Changed Services                         │
│     ├─ Check: services/catalog-service/**           │
│     ├─ Check: services/pricing-service/**           │
│     └─ Result: [catalog-service]                    │
│                                                     │
│  2. Build & Test                                    │
│     ├─ Build catalog-service                        │
│     ├─ Run unit tests                               │
│     ├─ Run integration tests                        │
│     └─ Upload artifacts                             │
│                                                     │
│  3. Security Scan                                   │
│     ├─ SpotBugs (code quality)                      │
│     ├─ OWASP Dependency Check                       │
│     └─ Generate SBOM                                │
└──────────────────────┬──────────────────────────────┘
                       │
                       ↓ (on main/develop)
┌─────────────────────────────────────────────────────┐
│  Docker Build & Publish                             │
│                                                     │
│  1. Build Docker Image                              │
│     └─ docker build -t catalog-service:sha          │
│                                                     │
│  2. Push to GHCR                                    │
│     └─ ghcr.io/yourorg/catalog-service:main-sha     │
│                                                     │
│  3. Tag Variants                                    │
│     ├─ :latest                                      │
│     ├─ :main-abc123                                 │
│     └─ :v1.0.0                                      │
└──────────────────────┬──────────────────────────────┘
                       │
                       ↓ (on develop)
┌─────────────────────────────────────────────────────┐
│  Deploy to Staging                                  │
│                                                     │
│  1. Helm Upgrade                                    │
│     └─ helm upgrade catalog-service                 │
│                                                     │
│  2. Wait for Rollout                                │
│     └─ kubectl rollout status                       │
│                                                     │
│  3. Smoke Tests                                     │
│     ├─ Health check                                 │
│     ├─ API test                                     │
│     └─ Integration test                             │
│                                                     │
│  4. Notify                                          │
│     └─ Slack: ✅ Deployment successful              │
└─────────────────────────────────────────────────────┘
```

---

## GitHub Actions Workflows

### 1. CI Build & Test (`ci-build.yml`)

**Purpose:** Build and test only changed services  
**Trigger:** Push to any branch, Pull requests

**Features:**
- ✅ **Path Filtering** - Detect which services changed
- ✅ **Parallel Builds** - Build multiple services concurrently
- ✅ **Maven Caching** - Speed up builds
- ✅ **Artifact Upload** - Save JARs for later stages

**How It Works:**

```yaml
# Detect changes
detect-changes:
  outputs:
    catalog: ${{ steps.filter.outputs.catalog }}
    pricing: ${{ steps.filter.outputs.pricing }}
    # ...

# Build only if changed
build-catalog-service:
  needs: detect-changes
  if: needs.detect-changes.outputs.catalog == 'true'
  steps:
    - Build with Maven
    - Run tests
    - Upload artifact
```

**Example:**
```bash
# Developer changes catalog-service
git commit -m "feat: add product search"
git push

# GitHub Actions:
# ✅ Detect change in services/catalog-service/
# ✅ Build catalog-service (5 min)
# ❌ Skip pricing-service, cart-service (no changes)
# ✅ Run tests
# ✅ Upload artifact
```

### 2. Docker Build & Publish (`docker-publish.yml`)

**Purpose:** Build Docker images and push to GitHub Container Registry  
**Trigger:** Push to main/develop branches

**Features:**
- ✅ **Multi-arch Builds** - AMD64, ARM64 support
- ✅ **Layer Caching** - GitHub Actions cache
- ✅ **Image Tagging** - Branch, SHA, semver tags
- ✅ **SBOM Generation** - Security compliance

**Image Tags:**
```
ghcr.io/yourorg/catalog-service:main-abc123   # Branch + SHA
ghcr.io/yourorg/catalog-service:latest        # Latest main
ghcr.io/yourorg/catalog-service:v1.0.0        # Semver tag
```

**How to Use:**

```bash
# Local: Pull published image
docker pull ghcr.io/yourorg/catalog-service:latest

# Kind: Load image
kind load docker-image ghcr.io/yourorg/catalog-service:latest

# Kubernetes: Deploy
kubectl set image deployment/catalog-service \
  catalog-service=ghcr.io/yourorg/catalog-service:main-abc123
```

### 3. Deploy to Staging (`deploy-staging.yml`)

**Purpose:** Automatically deploy to staging environment  
**Trigger:** Push to develop branch, Manual dispatch

**Features:**
- ✅ **Helm Upgrade** - Zero-downtime deployment
- ✅ **Rollout Status** - Wait for pods to be ready
- ✅ **Smoke Tests** - Verify deployment health
- ✅ **Slack Notifications** - Alert team on success/failure

**Manual Deployment:**

```yaml
# GitHub UI: Actions → Deploy to Staging → Run workflow
# Select service: catalog-service (or "all")
# Click: Run workflow
```

**Automated:**
```bash
# Push to develop
git checkout develop
git merge feature/new-feature
git push

# GitHub Actions:
# ✅ Build Docker image
# ✅ Push to GHCR
# ✅ Deploy to staging namespace
# ✅ Wait for rollout
# ✅ Run smoke tests
# ✅ Notify Slack
```

---

## Build-on-Change Strategy

### Path Filtering

**Purpose:** Only build services that changed, not entire monorepo.

**Use:** Save CI/CD time and resources.

**Configuration:**

```yaml
filters: |
  catalog:
    - 'services/catalog-service/**'
  pricing:
    - 'services/pricing-service/**'
```

**How It Works:**

```
Scenario 1: Change catalog-service
├─ Git diff detects: services/catalog-service/src/...
├─ Filter matches: catalog: true
├─ Build: catalog-service only
└─ Skip: pricing-service, cart-service, etc.

Scenario 2: Change pricing-service and cart-service
├─ Git diff detects: services/pricing-service/..., services/cart-service/...
├─ Filter matches: pricing: true, cart: true
├─ Build: pricing-service and cart-service in parallel
└─ Skip: catalog-service, order-service, etc.

Scenario 3: Change docs/
├─ Git diff detects: docs/...
├─ Filter matches: none
├─ Build: nothing
└─ Skip: all services (no code change)
```

**Benefits:**
- ✅ **Faster CI** - Average build time: 5-10 min (vs 60+ min for all services)
- ✅ **Lower Cost** - Only use CI minutes for changed code
- ✅ **Parallel Builds** - Multiple services build concurrently

---

## GitHub Container Registry (GHCR)

### Setup

**Purpose:** Host Docker images in GitHub ecosystem  
**Use:** Free, integrated with GitHub Actions, no external registry needed

### Enable GHCR

```bash
# 1. Generate Personal Access Token (PAT)
# GitHub → Settings → Developer Settings → Personal Access Tokens
# Scopes: write:packages, read:packages

# 2. Add to repository secrets
# Repository → Settings → Secrets → Actions
# Name: CR_PAT
# Value: <your-pat>

# 3. Login locally
echo $CR_PAT | docker login ghcr.io -u USERNAME --password-stdin
```

### Publish Image

**Automated (via GitHub Actions):**
```yaml
# Already configured in docker-publish.yml
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    push: true
    tags: ghcr.io/${{ github.repository_owner }}/catalog-service:latest
```

**Manual:**
```bash
# Build
docker build -t ghcr.io/yourorg/catalog-service:v1.0.0 .

# Push
docker push ghcr.io/yourorg/catalog-service:v1.0.0
```

### Pull Image

```bash
# Public image
docker pull ghcr.io/yourorg/catalog-service:latest

# Private image (requires authentication)
echo $CR_PAT | docker login ghcr.io -u USERNAME --password-stdin
docker pull ghcr.io/yourorg/catalog-service:latest
```

### Image Visibility

**Make Public:**
```bash
# GitHub → Packages → catalog-service → Package settings
# Change visibility: Public
```

---

## Helm Deployment

### Staging Deployment

**Purpose:** Deploy to staging namespace for testing before production  
**Use:** Automated deployment on every develop branch push

### Configuration

```bash
# Repository secrets needed:
# - KUBECONFIG_STAGING: Base64-encoded kubeconfig
# - SLACK_WEBHOOK: Slack incoming webhook URL

# Generate base64 kubeconfig:
cat ~/.kube/config | base64
```

### Deploy Service

**Automated:**
```bash
# Push to develop
git push origin develop

# GitHub Actions will:
# 1. Build Docker image
# 2. Push to GHCR
# 3. Deploy to platform-core-staging namespace
# 4. Wait for rollout
# 5. Run smoke tests
```

**Manual:**
```bash
# GitHub UI
Actions → Deploy to Staging → Run workflow
Service: catalog-service
Click: Run workflow

# CLI
gh workflow run deploy-staging.yml -f service=catalog-service
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -n platform-core-staging

# Check services
kubectl get svc -n platform-core-staging

# Check Helm releases
helm list -n platform-core-staging

# View logs
kubectl logs -n platform-core-staging deployment/catalog-service -f
```

---

## Terraform Infrastructure

**Purpose:** Provision cloud infrastructure as code  
**Use:** Reproducible, version-controlled infrastructure

### Structure

```
terraform/
├── modules/
│   ├── kubernetes/       # EKS/GKE/AKS cluster
│   ├── networking/       # VPC, subnets, load balancers
│   └── database/         # RDS PostgreSQL
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── terraform.tfvars
│   ├── staging/
│   └── prod/
└── README.md
```

### Modules

#### 1. Kubernetes Module

**Purpose:** Create managed Kubernetes cluster

```hcl
# modules/kubernetes/main.tf
resource "aws_eks_cluster" "main" {
  name     = var.cluster_name
  role_arn = aws_iam_role.cluster.arn

  vpc_config {
    subnet_ids = var.subnet_ids
  }
}

resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.cluster_name}-nodes"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.subnet_ids

  scaling_config {
    desired_size = var.node_count
    max_size     = var.node_max_count
    min_size     = var.node_min_count
  }

  instance_types = [var.instance_type]
}
```

#### 2. Networking Module

**Purpose:** Create VPC, subnets, NAT gateway

```hcl
# modules/networking/main.tf
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project}-vpc"
  }
}

resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 4, count.index)
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project}-public-${count.index + 1}"
  }
}
```

#### 3. Database Module

**Purpose:** Create RDS PostgreSQL instance

```hcl
# modules/database/main.tf
resource "aws_db_instance" "postgres" {
  identifier           = "${var.project}-postgres"
  engine               = "postgres"
  engine_version       = "15.3"
  instance_class       = var.instance_class
  allocated_storage    = var.allocated_storage
  storage_encrypted    = true
  
  db_name  = var.database_name
  username = var.master_username
  password = var.master_password

  vpc_security_group_ids = [aws_security_group.db.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  backup_retention_period = var.backup_retention_days
  skip_final_snapshot     = var.skip_final_snapshot

  tags = {
    Name = "${var.project}-postgres"
  }
}
```

### Usage

**Initialize:**
```bash
cd terraform/environments/dev
terraform init
```

**Plan:**
```bash
terraform plan -var-file=terraform.tfvars
```

**Apply:**
```bash
terraform apply -var-file=terraform.tfvars
```

**Destroy:**
```bash
terraform destroy -var-file=terraform.tfvars
```

---

## Security & Compliance

### SBOM Generation

**Purpose:** Software Bill of Materials for supply chain security  
**Use:** Track all dependencies and vulnerabilities

**Automatic:**
```yaml
# Included in docker-publish.yml
- name: Generate SBOM
  uses: anchore/sbom-action@v0
  with:
    format: spdx-json
    output-file: sbom-catalog-service.spdx.json
```

**Manual:**
```bash
# Install syft
brew install anchore/syft/syft

# Generate SBOM
syft packages catalog-service:latest -o spdx-json > sbom.json
```

### Dependency Scanning

**OWASP Dependency Check:**
```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>8.4.0</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>
  </configuration>
</plugin>
```

```bash
# Run scan
mvn dependency-check:check
```

### SpotBugs (Static Analysis)

```xml
<!-- pom.xml -->
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.8.0</version>
</plugin>
```

```bash
# Run SpotBugs
mvn spotbugs:check
```

---

## Smoke Tests

**Purpose:** Quick validation after deployment  
**Use:** Ensure service is responding and healthy

**Script:** `scripts/ci/smoke-tests.sh`

```bash
#!/bin/bash
set -e

NAMESPACE=$1

echo "🧪 Running smoke tests for namespace: $NAMESPACE"

# Test 1: Check pods are running
echo "✓ Checking pod status..."
kubectl get pods -n $NAMESPACE | grep Running || exit 1

# Test 2: Health check endpoints
for service in catalog-service pricing-service cart-service; do
  echo "✓ Testing $service health..."
  kubectl run curl-test --rm -it --restart=Never --image=curlimages/curl \
    -- curl -f http://$service.$NAMESPACE:8080/actuator/health || exit 1
done

# Test 3: API endpoint test
echo "✓ Testing API endpoints..."
kubectl run curl-test --rm -it --restart=Never --image=curlimages/curl \
  -- curl -f http://catalog-service.$NAMESPACE:8080/api/products || exit 1

echo "✅ All smoke tests passed!"
```

---

## Notifications

### Slack Integration

**Purpose:** Alert team on deployment success/failure  
**Use:** Keep team informed without checking GitHub

**Setup:**
```bash
# 1. Create Slack App
# https://api.slack.com/apps → Create New App

# 2. Enable Incoming Webhooks
# App → Incoming Webhooks → Activate

# 3. Add Webhook URL to GitHub Secrets
# Repository → Settings → Secrets → SLACK_WEBHOOK
```

**Message Format:**
```json
{
  "text": "✅ Deployment to staging successful",
  "blocks": [{
    "type": "section",
    "text": {
      "type": "mrkdwn",
      "text": "*Deployment Status:* Success\n*Environment:* Staging\n*Commit:* abc123\n*Author:* developer"
    }
  }]
}
```

---

## Best Practices

### 1. Semantic Commit Messages

```bash
# Format: <type>(<scope>): <subject>

feat(catalog): add product search
fix(pricing): correct discount calculation
chore(ci): update GitHub Actions versions
docs(readme): add deployment guide
```

**Benefits:**
- Auto-generate changelogs
- Trigger appropriate CI jobs
- Clear history

### 2. Branch Strategy

```
main
├─ production deployments
├─ protected branch
└─ requires PR reviews

develop
├─ staging deployments
├─ integration branch
└─ auto-deploy on push

feature/*
├─ development branches
└─ CI build & test only
```

### 3. Environment Variables

```yaml
# Never commit secrets!
# Use GitHub Secrets for:
# - KUBECONFIG_STAGING
# - KUBECONFIG_PROD
# - SLACK_WEBHOOK
# - CR_PAT

# Use environment-specific configs:
dev:
  replicas: 1
  resources:
    limits:
      memory: 512Mi

staging:
  replicas: 2
  resources:
    limits:
      memory: 1Gi

prod:
  replicas: 5
  resources:
    limits:
      memory: 2Gi
```

### 4. Rollback Strategy

```bash
# Helm rollback
helm rollback catalog-service -n platform-core-staging

# Kubernetes rollback
kubectl rollout undo deployment/catalog-service -n platform-core-staging

# Deploy specific version
helm upgrade catalog-service ./helm/charts/catalog-service \
  --set image.tag=main-abc123 \
  -n platform-core-staging
```

---

## Troubleshooting

### Workflow Not Triggering

```yaml
# Check path filters are correct
filters: |
  catalog:
    - 'services/catalog-service/**'  # Include subdirectories
```

### Docker Build Fails

```bash
# Check Dockerfile exists
ls services/catalog-service/Dockerfile

# Test build locally
docker build -t catalog-service:test services/catalog-service/
```

### Helm Deployment Fails

```bash
# Check namespace exists
kubectl get namespace platform-core-staging

# Check Helm chart syntax
helm lint ./helm/charts/catalog-service

# Dry-run deployment
helm upgrade --install catalog-service ./helm/charts/catalog-service \
  --dry-run --debug
```

### GHCR Push Permission Denied

```bash
# Check PAT has write:packages scope
# Check repository settings allow packages

# Re-login
echo $CR_PAT | docker login ghcr.io -u USERNAME --password-stdin
```

---

## Week 15 Summary

### Completed
- ✅ CI build workflow with path filtering
- ✅ Docker build and publish to GHCR
- ✅ Helm deployment to staging
- ✅ Terraform infrastructure scaffolding
- ✅ SBOM generation
- ✅ Security scanning (SpotBugs, OWASP)
- ✅ Smoke tests
- ✅ Slack notifications

### Key Achievements
- **Build Efficiency**: Only changed services build (5-10 min vs 60+ min)
- **Automated Deployment**: Push to develop → auto-deploy to staging
- **Security**: SBOM, dependency scanning, static analysis
- **Observability**: Slack notifications, deployment status

### Next Steps
- Week 16: SLOs & Alerts
- Week 17: Production deployment pipeline

---

**Last Updated:** December 12, 2025  
**Version:** 0.1.0  
**Status:** Week 15 Complete ✅

