
# E-Commerce Microservices Platform

A complete, production-grade, cloud-native backend built for mastering Java, Spring Boot, microservices, and distributed systems.

This repository contains a full-scale e-commerce backend platform made up of 26+ microservices, designed to emulate real-world enterprise architecture used by modern digital teams.

The purpose of this platform is to help you gain mastery in:

- Java 17 / Spring Boot 3  
- Microservices architecture  
- Event-driven design (Kafka, Avro, Schema Registry)  
- Async communication and Sagas  
- Payment workflows  
- Dynamic pricing  
- Inventory management and reservation  
- Resilience engineering  
- Distributed caches and TTL strategies  
- API Gateway  
- Observability and tracing  
- Kubernetes deployment  
- CI/CD pipelines  
- Security and identity management  

This platform is designed for learning, portfolio building, professional upskilling, and content creation (YouTube, Medium, GitHub).

---

## 🚀 Platform Overview

This system models a realistic e-commerce backend with capabilities such as:

- Product catalog  
- Search (Elasticsearch)  
- Dynamic pricing  
- Shopping cart  
- Checkout  
- Payment processing  
- Order orchestration  
- Inventory reservation  
- Notifications  
- Batch jobs  
- Feature flag management  
- Chaos engineering  
- Observability stack  
- CI/CD pipeline  
- Kubernetes-native deployment  

All microservices are independently deployable and communicate via REST + Kafka.

---

## 🧱 Repository Structure

```

ecommerce-platform/
│
├── README.md
│
├── docs/
│   ├── architecture/
│   ├── decisions/
│   ├── api/
│   ├── events/
│   └── diagrams/
│
├── services/
│   ├── catalog-service/
│   ├── cart-service/
│   ├── pricing-service/
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── search-service/
│   ├── batch-service/
│   ├── feature-flag-service/
│   ├── chaos-service/
│   ├── api-gateway/
│   ├── service-discovery/
│   ├── resilience-layer/
│   ├── security-service/
│   ├── schema-registry-extension/
│   ├── load-shedding-service/
│   └── ...
│
├── platform-libraries/
│   ├── common-dtos/
│   ├── shared-utils/
│   ├── tracing/
│   ├── logging/
│   └── resilience-config/
│
├── infra/
│   ├── kafka/
│   ├── schema-registry/
│   ├── postgres/
│   ├── redis/
│   ├── elasticsearch/
│   ├── keycloak/
│   ├── prometheus/
│   ├── grafana/
│   └── jaeger/
│
├── deployment/
│   ├── helm/
│   ├── k8s/
│   ├── docker/
│   └── terraform/
│
└── tests/
├── contract-tests/
├── load-tests/
├── e2e-tests/
└── testcontainers/

```

---

## 🧩 List of Microservices

### Core Domain Services
- Catalog Service  
- Cart Service  
- Pricing Service  
- Order Service  
- Payment Service  
- Inventory Service  
- Notification Service  
- Search Service  

### Platform Services
- API Gateway  
- Outbox + CDC Service  
- Feature Flag Service  
- Chaos Service  
- Batch Service  
- Schema Registry Extension  
- Resilience Layer  
- Security Service  
- Service Discovery (optional)  
- Load Shedding Service  

---

## 🔥 Key Architectural Patterns

### Microservices
- Independent deployability  
- Clear service boundaries  
- Domain-driven isolation  

### Async Event-Driven Architecture
- Kafka topics  
- Avro schemas  
- Schema Registry  
- Outbox + Debezium CDC  
- Event versioning governance  

### Resilience Engineering
- Circuit Breakers  
- Retries with exponential backoff  
- Bulkheads  
- Timeouts  
- Load shedding  
- Graceful degradation  
- Chaos tests  

### Distributed Systems
- Idempotency  
- Sagas  
- Eventual consistency  
- Caching strategies  
- Distributed tracing  
- Inventory locking patterns  

### DevOps & Cloud-Native
- Kubernetes  
- Helm charts  
- Ingress  
- Auto-scaling (HPA)  
- Observability (Otel, Prometheus, Grafana)  
- Vault secrets  
- Terraform IaC  
- CI/CD pipelines  

---

## 📐 Architecture Diagrams

Stored in `docs/architecture/`:

- System context  
- Microservice map  
- Checkout sequence  
- Payment callback sequence  
- Event architecture  
- Deployment architecture  
- Observability pipelines  

---

## ⛓ API Specifications

Located in `docs/api/`:

- catalog-openapi.yml  
- order-openapi.yml  
- pricing-openapi.yml  
- payment-openapi.yml  
- inventory-openapi.yml  
- cart-openapi.yml  

---

## 🧪 Testing Strategy

- Unit tests – JUnit, Mockito  
- Integration tests – Testcontainers  
- Contract tests – Pact  
- E2E tests – Local Kubernetes environment  
- Load tests – k6  
- Chaos tests – automated fault injection  

---

## 🛡 Security

- OAuth2 / OIDC with Keycloak  
- JWT validation  
- Vault-based secret management  
- No PII or sensitive data in logs  
- K8s RBAC & network policies  
- SAST/DAST scanning  

---

## 🚀 Running Locally

### Option 1: Docker Compose
```

docker-compose up

```

### Option 2: Kubernetes (Kind/Minikube)
```

helm install catalog-service deployment/helm/catalog-service

```

---

## 🗺 Roadmap

Planned future enhancements:

- Recommendation Service  
- Promotions & Coupon Service  
- Fraud Detection Service  
- Shipping Service  
- Warehouse Management  
- GraphQL Gateway  
- Real-time analytics pipeline  

---

## ⭐ Why This Platform Exists

To build:

- deep backend engineering expertise  
- real distributed-system experience  
- readiness for enterprise digital teams  
- a strong public technical portfolio  
- long-term content creation for Medium and YouTube
