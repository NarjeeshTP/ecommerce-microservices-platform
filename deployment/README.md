# Deployment

This directory contains all deployment-related configurations and scripts for the E-Commerce Microservices Platform.

## 📁 Directory Structure

```
deployment/
├── helm/                    # Helm charts for Kubernetes deployment
│   └── charts/
│       ├── catalog-service/
│       ├── pricing-service/
│       ├── cart-service/
│       └── ...
│
├── k8s/                     # Kubernetes manifests
│   ├── scripts/             # Setup and utility scripts
│   │   ├── setup-kind.sh
│   │   ├── setup-minikube.sh
│   │   └── kind-cluster-config.yaml
│   ├── namespaces/          # Namespace definitions
│   ├── base/                # Base Kubernetes resources
│   │   └── infrastructure/  # Infrastructure components
│   └── service-mesh/        # Istio configuration
│       ├── scripts/
│       └── resilience-policies/
│
├── slo-alerts/              # SLO monitoring and alerting
│   ├── prometheus/          # Prometheus alert rules
│   ├── grafana/             # Grafana dashboards
│   ├── runbooks/            # Incident response runbooks
│   └── chaos-experiments/   # Chaos testing scripts
│
├── docker/                  # Docker-related files
│   ├── docker-compose.yml   # Local development compose
│   └── Dockerfiles/         # Service-specific Dockerfiles
│
└── terraform/               # Infrastructure as Code
    ├── modules/             # Terraform modules
    │   ├── kubernetes/
    │   ├── networking/
    │   └── database/
    └── environments/        # Environment-specific configs
        ├── dev/
        ├── staging/
        └── prod/
```

## 🚀 Quick Start

### Local Development (Kind)

```bash
# 1. Create Kind cluster
cd deployment/k8s/scripts
./setup-kind.sh

# 2. Deploy infrastructure
kubectl apply -f deployment/k8s/base/infrastructure/

# 3. Install Istio service mesh
cd deployment/k8s/service-mesh/scripts
./install-istio.sh

# 4. Deploy services with Helm
helm install catalog-service deployment/helm/charts/catalog-service -n platform-core
helm install pricing-service deployment/helm/charts/pricing-service -n platform-core
```

### Cloud Deployment (Terraform)

```bash
# 1. Initialize Terraform
cd deployment/terraform/environments/dev
terraform init

# 2. Plan infrastructure
terraform plan -var-file=terraform.tfvars

# 3. Apply infrastructure
terraform apply -var-file=terraform.tfvars

# 4. Deploy services via CI/CD
# GitHub Actions workflows will handle deployment
```

## 📚 Documentation

### Helm Charts
- [Helm Chart Guide](helm/README.md) - Helm chart structure and usage (if exists)

### Kubernetes
- [Kubernetes Setup](k8s/README.md) - Kind/Minikube setup and K8s resources
- [Service Mesh](k8s/service-mesh/README.md) - Istio installation and resilience policies

### SLO Monitoring & Alerting
- [SLO & Alerts Guide](slo-alerts/README.md) - SLO definitions, Prometheus alerts, incident runbooks

### Terraform
- [Infrastructure Guide](terraform/README.md) - Terraform modules and environment setup (if exists)

## 🔧 Tools Required

### Local Development
- Docker Desktop
- kubectl (v1.28+)
- Helm (v3.13+)
- Kind (v0.20+) or Minikube

### Cloud Deployment
- Terraform (v1.5+)
- Cloud CLI (AWS CLI, gcloud, or Azure CLI)
- kubectl configured for cloud cluster

## 🎯 Deployment Targets

### Development
- **Local:** Kind/Minikube cluster
- **Services:** All services with mock dependencies
- **Database:** PostgreSQL in Docker
- **Observability:** Grafana + Prometheus locally

### Staging
- **Cloud:** EKS/GKE/AKS cluster
- **Services:** All services with real dependencies
- **Database:** RDS/Cloud SQL
- **Namespace:** `platform-core-staging`
- **Auto-deploy:** On push to `develop` branch

### Production
- **Cloud:** Multi-region EKS/GKE/AKS
- **Services:** All services with HA configuration
- **Database:** RDS/Cloud SQL with read replicas
- **Namespace:** `platform-core-prod`
- **Deploy:** Manual approval required

## 🚦 CI/CD Pipeline

```
Code Push
   ↓
GitHub Actions (Build & Test)
   ↓
Docker Build & Push to GHCR
   ↓
Staging Deployment (automatic)
   ↓
Smoke Tests
   ↓
Production Deployment (manual approval)
```

See [CI/CD Guide](../.github/README.md) for detailed pipeline documentation.

## 🔐 Secrets Management

### Local Development
- Environment variables in `.env` files
- Docker secrets for sensitive data

### Cloud Deployment
- Kubernetes secrets for service credentials
- AWS Secrets Manager / HashiCorp Vault for external secrets
- Sealed Secrets for Git-committed encrypted secrets

## 📊 Monitoring & Observability

All deployment targets include:
- **Metrics:** Prometheus + Grafana
- **Tracing:** Jaeger
- **Logging:** EFK/ELK stack
- **Alerting:** Alertmanager + PagerDuty/Slack
- **SLO Monitoring:** Error budget tracking, burn rate alerts (see `slo-alerts/`)

## 🧪 Testing Deployment

```bash
# Verify cluster
kubectl get nodes
kubectl get namespaces | grep platform

# Check deployments
kubectl get deployments -n platform-core
kubectl get pods -n platform-core

# Test service endpoints
kubectl port-forward -n platform-core svc/catalog-service 8080:8080
curl http://localhost:8080/actuator/health

# Run smoke tests
./scripts/ci/smoke-tests.sh platform-core
```

## 🔄 Rollback Procedures

### Helm Rollback
```bash
# View history
helm history catalog-service -n platform-core

# Rollback to previous
helm rollback catalog-service -n platform-core

# Rollback to specific version
helm rollback catalog-service 2 -n platform-core
```

### Kubernetes Rollback
```bash
# View rollout history
kubectl rollout history deployment/catalog-service -n platform-core

# Rollback
kubectl rollout undo deployment/catalog-service -n platform-core
```

## 🐛 Troubleshooting

### Pods not starting
```bash
# Check pod status
kubectl describe pod <pod-name> -n platform-core

# View logs
kubectl logs <pod-name> -n platform-core -f

# Check events
kubectl get events -n platform-core --sort-by='.lastTimestamp'
```

### Service connectivity issues
```bash
# Test service DNS
kubectl run curl-test --rm -it --image=curlimages/curl -- \
  curl http://catalog-service.platform-core:8080/actuator/health

# Check endpoints
kubectl get endpoints -n platform-core
```

### Istio issues
```bash
# Check sidecar injection
kubectl get pods -n platform-core -o jsonpath='{.items[*].spec.containers[*].name}'

# Verify Istio configuration
istioctl analyze -n platform-core

# Check Envoy logs
kubectl logs <pod-name> -n platform-core -c istio-proxy
```

## 📝 Best Practices

1. **Always test in staging before production**
2. **Use Helm for service deployment** (consistent, versioned)
3. **Enable Istio sidecar injection** for all services
4. **Set resource limits** on all deployments
5. **Use namespaces** to separate environments
6. **Enable auto-scaling (HPA)** for production
7. **Monitor error budgets** before deploying
8. **Have rollback plan** before every deployment

## 🆘 Support

- Kubernetes issues: See [k8s/README.md](k8s/README.md)
- Helm issues: See [helm/README.md](helm/README.md)
- Terraform issues: See [terraform/README.md](terraform/README.md)
- SLO & Alerting: See [slo-alerts/README.md](slo-alerts/README.md)
- CI/CD issues: See [../.github/README.md](../.github/README.md)

---

**Last Updated:** December 12, 2025  
**Maintainer:** Platform Team

