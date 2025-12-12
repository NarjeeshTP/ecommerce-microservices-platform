# Kubernetes Deployment Guide

Complete Kubernetes deployment guide for E-Commerce Microservices Platform with Kind, Minikube, and Helm charts.

## ✅ Current Status (Dec 11, 2025)

**Version:** 0.1.0  
**Status:** Week 13 Implementation Complete

### Features Implemented
- ✅ **Namespace Definitions** - platform-system, platform-core, platform-infra
- ✅ **Kind Cluster Setup** - Local development with 3 nodes
- ✅ **Minikube Setup** - Alternative local Kubernetes
- ✅ **Helm Charts** - Template charts for services
- ✅ **Ingress Configuration** - NGINX Ingress Controller
- ✅ **Resource Management** - CPU/Memory requests and limits
- ✅ **Auto-scaling** - Horizontal Pod Autoscaler (HPA)
- ✅ **Health Checks** - Liveness and readiness probes

---

## Architecture

### Namespace Organization

**Purpose:** Organize services into logical groups for isolation, security, and resource management.

```
┌─────────────────────────────────────────────────────┐
│  platform-system (System Components)                │
│  Purpose: Gateway and control services              │
│  Use: Route traffic, manage features, test chaos    │
│                                                     │
│  - API Gateway      → Single entry point            │
│  - Feature Flags    → Toggle features on/off       │
│  - Chaos Service    → Inject failures for testing  │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  platform-core (Business Services)                  │
│  Purpose: Core business logic services             │
│  Use: Handle customer requests and transactions    │
│                                                     │
│  - Catalog Service     → Product listings          │
│  - Pricing Service     → Price calculations        │
│  - Cart Service        → Shopping cart             │
│  - Order Service       → Order processing          │
│  - Payment Service     → Payment handling          │
│  - Inventory Service   → Stock management          │
│  - Notification Service → Emails/SMS              │
│  - Search Service      → Product search            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  platform-infra (Infrastructure)                    │
│  Purpose: Backend infrastructure services           │
│  Use: Data storage, messaging, monitoring           │
│                                                     │
│  - Kafka + Zookeeper → Event streaming             │
│  - Redis             → Caching                     │
│  - PostgreSQL        → Data storage                │
│  - Elasticsearch     → Search indexing             │
│  - Prometheus        → Metrics collection          │
│  - Grafana           → Metrics visualization       │
│  - Jaeger            → Distributed tracing         │
└─────────────────────────────────────────────────────┘
```

**Why Separate Namespaces?**
- ✅ **Isolation** - Services don't interfere with each other
- ✅ **Security** - Different access controls per namespace
- ✅ **Resources** - Set CPU/memory limits per namespace
- ✅ **Organization** - Easy to find and manage services

---

## Prerequisites

### Required Tools

```bash
# Install Kind
brew install kind
# Or download from: https://kind.sigs.k8s.io/docs/user/quick-start/#installation

# Install Minikube (alternative to Kind)
brew install minikube

# Install kubectl
brew install kubectl

# Install Helm
brew install helm

# Verify installations
kind version
minikube version
kubectl version --client
helm version
```

### System Requirements

**For Kind:**
- Docker Desktop running
- 4 CPU cores minimum
- 8GB RAM minimum
- 20GB disk space

**For Minikube:**
- Docker or VirtualBox
- 4 CPU cores minimum
- 8GB RAM minimum
- 40GB disk space

---

## Quick Start

### Option 1: Kind (Recommended for Development)

```bash
# 1. Create Kind cluster
cd deployment/k8s/scripts
./setup-kind.sh

# 2. Verify cluster
kubectl get nodes
kubectl get namespaces | grep platform

# 3. Deploy infrastructure
kubectl apply -f deployment/k8s/base/infrastructure/

# 4. Deploy services
helm install catalog-service deployment/helm/charts/catalog-service \
  -n platform-core \
  --create-namespace

# 5. Check status
kubectl get all -n platform-core
```

### Option 2: Minikube

```bash
# 1. Create Minikube cluster
cd deployment/k8s/scripts
./setup-minikube.sh

# 2. Start tunnel (in separate terminal)
minikube tunnel

# 3. Deploy same as Kind
kubectl apply -f deployment/k8s/base/infrastructure/
helm install catalog-service deployment/helm/charts/catalog-service -n platform-core

# 4. Access dashboard
minikube dashboard
```

---

## Kind Cluster Setup

**Purpose:** Run Kubernetes locally using Docker containers (faster than VMs).

**Use Cases:**
- ✅ Local development and testing
- ✅ CI/CD pipelines
- ✅ Quick cluster creation/deletion
- ✅ Multi-node testing

### Configuration

**File:** `deployment/k8s/scripts/kind-cluster-config.yaml`

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: ecommerce-platform
nodes:
  - role: control-plane  # API server, etcd, scheduler
                         # Purpose: Manages cluster, stores state
    extraPortMappings:
    - containerPort: 80    # HTTP
      hostPort: 80         # Use: Access services via localhost:80
    - containerPort: 443   # HTTPS
      hostPort: 443        # Use: Access services via localhost:443
  - role: worker          # Application pods
                          # Purpose: Runs your services
  - role: worker          # Application pods
                          # Purpose: Load balancing, high availability
```

**Features:**
- 1 control-plane node (manages cluster)
- 2 worker nodes (runs applications)
- Port forwarding for ingress (80, 443)
- Suitable for local development

### Create Cluster

```bash
# Create
kind create cluster --config deployment/k8s/scripts/kind-cluster-config.yaml

# Verify
kubectl cluster-info --context kind-ecommerce-platform
kubectl get nodes

# Output:
# NAME                                 STATUS   ROLES
# ecommerce-platform-control-plane     Ready    control-plane
# ecommerce-platform-worker            Ready    <none>
# ecommerce-platform-worker2           Ready    <none>
```

### Load Docker Images

```bash
# Build image
docker build -t catalog-service:latest services/catalog-service

# Load into Kind
kind load docker-image catalog-service:latest --name ecommerce-platform

# Verify
docker exec -it ecommerce-platform-control-plane crictl images | grep catalog
```

### Delete Cluster

```bash
kind delete cluster --name ecommerce-platform
```

---

## Minikube Setup

**Purpose:** Run single-node Kubernetes cluster locally with built-in dashboard.

**Use Cases:**
- ✅ Learning Kubernetes
- ✅ Local development with GUI
- ✅ Testing on single node
- ✅ Easy addon installation

**When to Use:**
- Beginners (has dashboard)
- Need visual monitoring
- Single-node testing sufficient

### Create Cluster

```bash
# Start with addons
minikube start \
  --cpus=4 \              # Use: Allocate 4 CPU cores
  --memory=8192 \         # Use: Allocate 8GB RAM
  --disk-size=40gb \      # Use: Allocate 40GB storage
  --driver=docker \       # Use: Run in Docker (faster than VM)
  --addons=ingress,metrics-server,dashboard
  # Purpose: Enable ingress routing, metrics, and web UI

# Verify
minikube status
kubectl get nodes

# Get IP
minikube ip
# Example: 192.168.49.2
# Use: Access services at this IP address
```

### Access Services

```bash
# Option 1: Tunnel (recommended)
minikube tunnel
# Now services are accessible at localhost

# Option 2: NodePort
kubectl get svc -n platform-core
# Access at: http://<minikube-ip>:<NodePort>

# Option 3: Port Forward
kubectl port-forward -n platform-core svc/catalog-service 8080:8080
```

### Dashboard

```bash
# Open dashboard
minikube dashboard

# Or get URL
minikube dashboard --url
```

---

## Namespaces

### Definitions

**File:** `deployment/k8s/namespaces/namespaces.yaml`

```yaml
---
apiVersion: v1
kind: Namespace
metadata:
  name: platform-system
  labels:
    tier: system
    monitoring: enabled

---
apiVersion: v1
kind: Namespace
metadata:
  name: platform-core
  labels:
    tier: application
    monitoring: enabled

---
apiVersion: v1
kind: Namespace
metadata:
  name: platform-infra
  labels:
    tier: infrastructure
    monitoring: enabled
```

### Create Namespaces

```bash
# Apply
kubectl apply -f deployment/k8s/namespaces/namespaces.yaml

# Verify
kubectl get namespaces --show-labels | grep platform

# Output:
# platform-system   Active   tier=system
# platform-core     Active   tier=application
# platform-infra    Active   tier=infrastructure
```

### Usage

```bash
# Deploy to specific namespace
kubectl apply -f deployment.yaml -n platform-core

# Get resources in namespace
kubectl get all -n platform-core

# Describe namespace
kubectl describe namespace platform-core

# Delete namespace (⚠️  deletes all resources)
kubectl delete namespace platform-core
```

---

## Helm Charts

**Purpose:** Package Kubernetes applications for easy deployment and management.

**Use Cases:**
- ✅ Reusable templates across services
- ✅ Environment-specific configurations (dev/prod)
- ✅ Version control for releases
- ✅ Easy rollback to previous versions

**Why Use Helm:**
- **Simplicity** - Deploy with one command
- **Reusability** - One chart for multiple environments
- **Versioning** - Track deployment history
- **Rollback** - Easily revert to previous version

### Chart Structure

```
deployment/helm/charts/catalog-service/
├── Chart.yaml              # Chart metadata
                            # Purpose: Name, version, description
├── values.yaml             # Default values
                            # Purpose: Default configuration
├── values-dev.yaml         # Dev environment
                            # Purpose: Dev-specific overrides
├── values-prod.yaml        # Prod environment
                            # Purpose: Production settings
└── templates/
    ├── deployment.yaml     # Deployment spec
                            # Purpose: Define pods to run
    ├── service.yaml        # Service spec
                            # Purpose: Network access to pods
    ├── ingress.yaml        # Ingress rules
                            # Purpose: External routing
    ├── configmap.yaml      # Configuration
                            # Purpose: Non-sensitive config
    ├── secret.yaml         # Secrets
                            # Purpose: Passwords, tokens
    ├── hpa.yaml            # Auto-scaling
                            # Purpose: Scale based on load
    ├── serviceaccount.yaml # Service account
                            # Purpose: Pod permissions
    └── _helpers.tpl        # Template helpers
                            # Purpose: Reusable functions
```

### Install Chart

```bash
# Install with default values
helm install catalog-service deployment/helm/charts/catalog-service \
  -n platform-core \
  --create-namespace

# Install with custom values
helm install catalog-service deployment/helm/charts/catalog-service \
  -n platform-core \
  -f deployment/helm/charts/catalog-service/values-dev.yaml

# Install with overrides
helm install catalog-service deployment/helm/charts/catalog-service \
  -n platform-core \
  --set image.tag=v1.0.0 \
  --set replicaCount=3
```

### Upgrade Chart

```bash
# Upgrade
helm upgrade catalog-service deployment/helm/charts/catalog-service \
  -n platform-core

# Upgrade with new values
helm upgrade catalog-service deployment/helm/charts/catalog-service \
  -n platform-core \
  -f deployment/helm/charts/catalog-service/values-prod.yaml

# Rollback
helm rollback catalog-service 1 -n platform-core
```

### Manage Charts

```bash
# List releases
helm list -n platform-core

# Get values
helm get values catalog-service -n platform-core

# Get manifest
helm get manifest catalog-service -n platform-core

# Uninstall
helm uninstall catalog-service -n platform-core
```

---

## Deployment Configuration

### Sample Deployment

**File:** `deployment/helm/charts/catalog-service/templates/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "catalog-service.fullname" . }}
  namespace: {{ .Values.namespace }}
  labels:
    {{- include "catalog-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "catalog-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
      labels:
        {{- include "catalog-service.selectorLabels" . | nindent 8 }}
    spec:
      serviceAccountName: {{ include "catalog-service.serviceAccountName" . }}
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: {{ .Values.springProfile }}
        - name: DB_HOST
          value: {{ .Values.database.host }}
        - name: DB_PORT
          value: "{{ .Values.database.port }}"
        - name: REDIS_HOST
          value: {{ .Values.redis.host }}
        resources:
          {{- toYaml .Values.resources | nindent 12 }}
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: http
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: http
          initialDelaySeconds: 30
          periodSeconds: 5
```

### Sample Values

**File:** `deployment/helm/charts/catalog-service/values.yaml`

```yaml
replicaCount: 2

image:
  repository: catalog-service
  pullPolicy: IfNotPresent
  tag: "latest"

namespace: platform-core

springProfile: kubernetes

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  className: nginx
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
  hosts:
    - host: api.ecommerce.local
      paths:
        - path: /api/catalog
          pathType: Prefix

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

database:
  host: postgres-service.platform-infra.svc.cluster.local
  port: 5432
  name: catalogdb

redis:
  host: redis-service.platform-infra.svc.cluster.local
  port: 6379
```

---

## Service Configuration

**Purpose:** Expose pods on the network for access by other services or external clients.

### ClusterIP Service (Internal)

**Purpose:** Internal communication between services within cluster  
**Use:** Default service type for microservice communication

```yaml
apiVersion: v1
kind: Service
metadata:
  name: catalog-service
  namespace: platform-core
spec:
  type: ClusterIP
  selector:
    app: catalog-service
  ports:
    - name: http
      port: 8080        # Service port
      targetPort: 8080  # Pod port
```

**Use Case:** Inter-service communication (Order → Catalog)

**Access:** `http://catalog-service.platform-core.svc.cluster.local:8080`

**When to Use:**
- ✅ Service-to-service calls
- ✅ Internal APIs
- ✅ Default choice for most cases

### NodePort Service (External)

**Purpose:** Expose service on each node's IP at a static port  
**Use:** Simple external access for testing

```yaml
apiVersion: v1
kind: Service
metadata:
  name: catalog-service
  namespace: platform-core
spec:
  type: NodePort
  selector:
    app: catalog-service
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      nodePort: 30080  # 30000-32767 range
```

**Use Case:** External access without LoadBalancer (development/testing)

**Access:** `http://<node-ip>:30080`

**When to Use:**
- ✅ Local testing
- ✅ Development environment
- ✅ No cloud LoadBalancer available

### LoadBalancer Service (Cloud)

**Purpose:** Expose service via cloud provider's load balancer  
**Use:** Production external access with automatic load balancing

```yaml
apiVersion: v1
kind: Service
metadata:
  name: catalog-service
  namespace: platform-core
spec:
  type: LoadBalancer
  selector:
    app: catalog-service
  ports:
    - name: http
      port: 80          # External port
      targetPort: 8080  # Pod port
```

**Use Case:** Production cloud deployment (AWS ELB, GCP LB, Azure LB)

**Access:** External IP assigned by cloud provider (e.g., 34.123.45.67)

**When to Use:**
- ✅ Production on cloud (AWS, GCP, Azure)
- ✅ Need automatic load balancing
- ✅ Want managed external IP

---

## Ingress Configuration

**Purpose:** Route external HTTP/HTTPS traffic to internal services based on URL paths.

**Use:** Single entry point for multiple services (like API Gateway at network level)

### NGINX Ingress

**How It Works:**
```
Client Request: http://api.ecommerce.local/api/catalog/products
       ↓
NGINX Ingress Controller (matches rules)
       ↓
Routes to: catalog-service:8080/products
```

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: platform-ingress
  namespace: platform-core
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    # Purpose: Rewrite URL path before forwarding
    # Use: /api/catalog/products → /products
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
    # Purpose: Disable automatic HTTPS redirect
    # Use: Allow HTTP for local development
spec:
  ingressClassName: nginx
  # Purpose: Use NGINX as ingress controller
  rules:
  - host: api.ecommerce.local
    # Purpose: Match requests to this hostname
    # Use: Route api.ecommerce.local to services
    http:
      paths:
      - path: /api/catalog(/|$)(.*)
        # Purpose: Match /api/catalog/* paths
        # Use: Route catalog requests
        pathType: ImplementationSpecific
        backend:
          service:
            name: catalog-service
            port:
              number: 8080
      - path: /api/orders(/|$)(.*)
        # Purpose: Match /api/orders/* paths
        # Use: Route order requests
        pathType: ImplementationSpecific
        backend:
          service:
            name: order-service
            port:
              number: 8084
```

**Benefits:**
- ✅ Single external IP for all services
- ✅ Path-based routing (/api/catalog, /api/orders)
- ✅ SSL termination
- ✅ Load balancing across pods

### Local DNS Setup

```bash
# Add to /etc/hosts
echo "127.0.0.1 api.ecommerce.local" | sudo tee -a /etc/hosts

# Test
curl http://api.ecommerce.local/api/catalog/products
```

---

## Resource Management

**Purpose:** Control CPU and memory allocation for pods to prevent resource starvation.

**Use:** Ensure fair resource distribution and prevent one service from using all cluster resources.

### Resource Requests & Limits

```yaml
resources:
  requests:
    cpu: 250m        # Guaranteed CPU (0.25 cores)
                     # Purpose: Minimum CPU reserved for pod
                     # Use: Scheduler ensures node has this available
    memory: 512Mi    # Guaranteed memory
                     # Purpose: Minimum RAM reserved for pod
                     # Use: Prevents OOM (Out of Memory) kills
  limits:
    cpu: 1000m       # Max CPU (1 full core)
                     # Purpose: Maximum CPU pod can use
                     # Use: Prevents CPU hogging, throttles if exceeded
    memory: 1Gi      # Max memory
                     # Purpose: Maximum RAM pod can use
                     # Use: Pod killed if exceeded (OOMKilled)
```

**Best Practices:**
- Set requests = actual average usage
- Set limits = 2x requests (for bursts)
- Monitor actual usage in Grafana
- Adjust based on load tests

**What Happens:**
- **Requests:** Kubernetes scheduler finds node with available resources
- **Limits:** Container throttled (CPU) or killed (memory) if exceeded

### Quality of Service (QoS)

**Guaranteed:**
```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 250m
    memory: 512Mi
```
**Result:** Highest priority, never evicted

**Burstable:**
```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```
**Result:** Can burst, may be throttled

**BestEffort:**
```yaml
# No resources specified
```
**Result:** Lowest priority, evicted first

---

## Auto-scaling (HPA)

**Purpose:** Automatically scale number of pods based on CPU/memory usage.

**Use:** Handle traffic spikes without manual intervention, save resources during low traffic.

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: catalog-service-hpa
  namespace: platform-core
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: catalog-service
  minReplicas: 2           # Purpose: Minimum pods for availability
                           # Use: Always have 2 pods running
  maxReplicas: 10          # Purpose: Maximum pods to prevent runaway scaling
                           # Use: Cap at 10 pods max
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # Purpose: Scale when avg CPU > 70%
                                # Use: Add pods before hitting 100%
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80  # Purpose: Scale when avg memory > 80%
                                # Use: Prevent OOM kills
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # Purpose: Wait 5 min before scaling down
                                       # Use: Avoid flapping during spikes
      policies:
      - type: Percent
        value: 50                      # Purpose: Remove 50% of pods at once
        periodSeconds: 60              # Use: Gradual scale-down
    scaleUp:
      stabilizationWindowSeconds: 0    # Purpose: Scale up immediately
                                       # Use: Fast response to traffic spikes
      policies:
      - type: Percent
        value: 100                     # Purpose: Double pods at once
        periodSeconds: 15              # Use: Rapid scale-up
```

**How It Works:**
1. Metrics Server collects CPU/memory from kubelet
2. HPA checks metrics every 15 seconds
3. If avg CPU > 70% → calculate new replicas → scale up
4. If avg CPU < 70% (after 5 min) → scale down slowly

**Example:**
```
Current: 2 replicas, 80% CPU
Target: 70% CPU
Calculation: ceil(2 * (80 / 70)) = ceil(2.28) = 3 replicas
Result: Scale from 2 → 3 pods
```

**Check Status:**
```bash
kubectl get hpa -n platform-core
# NAME                  REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS
# catalog-service-hpa   Deployment/catalog-svc   45%/70%   2         10        3
```

---

## Health Checks

**Purpose:** Monitor pod health and restart or remove from load balancing when unhealthy.

**Use:** Ensure only healthy pods serve traffic, automatically recover from failures.

### Liveness Probe

**Purpose:** Detect and restart unhealthy/deadlocked pods  
**Use:** Kubernetes kills and restarts pod if check fails

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness  # Purpose: Check if app is alive
                                     # Use: Spring Boot health endpoint
    port: 8080
  initialDelaySeconds: 60  # Purpose: Wait 60s after pod starts
                           # Use: Give app time to initialize
  periodSeconds: 10        # Purpose: Check every 10 seconds
                           # Use: Quick detection of problems
  timeoutSeconds: 3        # Purpose: Wait 3s for response
                           # Use: Detect hung processes
  failureThreshold: 3      # Purpose: Restart after 3 consecutive failures
                           # Use: Avoid false positives
```

**When to Use:**
- ✅ Detect deadlocks (threads stuck)
- ✅ Detect memory leaks (growing memory)
- ✅ Restart frozen processes
- ✅ Recover from unrecoverable errors

**Example Scenario:**
```
1. Pod starts → wait 60s
2. Check every 10s → GET /actuator/health/liveness
3. Failure 1 → log warning
4. Failure 2 → log warning
5. Failure 3 → RESTART pod
```

### Readiness Probe

**Purpose:** Remove pod from load balancing when not ready  
**Use:** Prevent traffic to pods that can't handle requests

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness  # Purpose: Check if ready for traffic
                                      # Use: DB connected, cache ready
    port: 8080
  initialDelaySeconds: 30  # Purpose: Wait 30s after start
                           # Use: Let app warm up
  periodSeconds: 5         # Purpose: Check every 5 seconds
                           # Use: Fast traffic routing changes
  timeoutSeconds: 3        # Purpose: Wait 3s for response
                           # Use: Detect slow responses
  failureThreshold: 3      # Purpose: Remove after 3 failures
                           # Use: Avoid flapping
```

**When to Use:**
- ✅ Wait for database connection
- ✅ Wait for cache warm-up
- ✅ Temporary overload (backpressure)
- ✅ Graceful shutdown

**Example Scenario:**
```
1. Pod starts → wait 30s
2. Check every 5s → GET /actuator/health/readiness
3. Ready → add to Service endpoints → receive traffic
4. Not ready → remove from Service → no traffic
5. Ready again → add back → receive traffic
```

**Difference from Liveness:**
- **Liveness:** Restarts pod (drastic)
- **Readiness:** Temporarily removes from load balancing (gentle)

### Startup Probe

**Purpose:** Allow slow-starting pods extra time before liveness checks  
**Use:** Prevent premature restarts for apps with long initialization

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness  # Purpose: Same as liveness
                                     # Use: Check if started
    port: 8080
  initialDelaySeconds: 0   # Purpose: Start checking immediately
                           # Use: Don't wait like liveness
  periodSeconds: 10        # Purpose: Check every 10 seconds
                           # Use: Allow time to start
  failureThreshold: 30     # Purpose: Allow 30 attempts (5 minutes)
                           # Use: 30 * 10s = 5 min startup time
```

**When to Use:**
- ✅ Large applications (Java with big classpath)
- ✅ Slow initialization (data loading)
- ✅ Cloud-native apps (service discovery)
- ✅ Prevents premature liveness failures

**Example Scenario:**
```
1. Pod starts
2. Startup probe checks every 10s for up to 5 minutes
3. Liveness/readiness probes DISABLED during startup
4. Once startup succeeds → enable liveness/readiness
5. If startup fails after 5 min → restart pod
```

**Why Need It:**
Without startup probe, liveness probe might restart pod before it finishes starting!

---

## Configuration Management

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: catalog-service-config
  namespace: platform-core
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/catalogdb
      redis:
        host: redis
        port: 6379
```

**Use in Pod:**
```yaml
envFrom:
- configMapRef:
    name: catalog-service-config
```

### Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: catalog-service-secret
  namespace: platform-core
type: Opaque
data:
  DB_PASSWORD: cG9zdGdyZXM=  # base64 encoded
stringData:
  DB_USER: postgres           # plain text
```

**Use in Pod:**
```yaml
env:
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: catalog-service-secret
      key: DB_PASSWORD
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n platform-core

# Describe pod
kubectl describe pod catalog-service-xxx -n platform-core

# Check logs
kubectl logs catalog-service-xxx -n platform-core

# Check previous logs (if restarted)
kubectl logs catalog-service-xxx -n platform-core --previous

# Common issues:
# - ImagePullBackOff: Wrong image name
# - CrashLoopBackOff: Application error
# - Pending: Resource constraints
```

### Service Not Accessible

```bash
# Check service
kubectl get svc -n platform-core

# Check endpoints
kubectl get endpoints catalog-service -n platform-core

# Port forward to test
kubectl port-forward -n platform-core svc/catalog-service 8080:8080
curl http://localhost:8080/actuator/health

# Common issues:
# - No endpoints: Selector mismatch
# - Connection refused: Wrong port
```

### Ingress Not Working

```bash
# Check ingress
kubectl get ingress -n platform-core

# Describe ingress
kubectl describe ingress platform-ingress -n platform-core

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Test from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://catalog-service.platform-core:8080/actuator/health
```

---

## Useful Commands

### Pod Management

```bash
# List pods
kubectl get pods -n platform-core

# Watch pods
kubectl get pods -n platform-core -w

# Delete pod (recreated by deployment)
kubectl delete pod catalog-service-xxx -n platform-core

# Execute command in pod
kubectl exec -it catalog-service-xxx -n platform-core -- /bin/sh

# Copy files
kubectl cp file.txt platform-core/catalog-service-xxx:/tmp/
```

### Logs

```bash
# Tail logs
kubectl logs -f catalog-service-xxx -n platform-core

# Logs from all pods
kubectl logs -l app=catalog-service -n platform-core

# Logs with timestamp
kubectl logs catalog-service-xxx -n platform-core --timestamps

# Last 100 lines
kubectl logs catalog-service-xxx -n platform-core --tail=100
```

### Resource Usage

```bash
# Pod resource usage
kubectl top pods -n platform-core

# Node resource usage
kubectl top nodes

# Describe node
kubectl describe node <node-name>
```

---

## Week 13 Summary

### Completed
- ✅ Namespace definitions (platform-system, platform-core, platform-infra)
- ✅ Kind cluster setup script
- ✅ Minikube cluster setup script
- ✅ Helm chart template (catalog-service)
- ✅ Ingress configuration
- ✅ Resource management
- ✅ Auto-scaling (HPA)
- ✅ Health checks configuration
- ✅ Comprehensive documentation

### Next Steps
- Deploy infrastructure to Kubernetes
- Convert all services to Helm charts
- Implement service mesh (Istio/Linkerd)
- Setup CI/CD for Kubernetes deployments

---

**Last Updated:** December 11, 2025  
**Version:** 0.1.0  
**Status:** Week 13 Complete ✅

