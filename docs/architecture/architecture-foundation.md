# Architecture Foundation — One Pager
- `services/template-service` scaffolded from the starter.
- `platform-libraries/service-starter` exists and can be used to create new services.
- Local infra (`infra/docker-compose.yml`) boots and essential endpoints respond (Schema Registry, Postgres, Redis).
- Architecture one-pager reviewed and accepted by the team.

Success criteria for Phase 1

3. Run integration tests against local infra (Testcontainers optional).
2. Start a service from `services/` (use `platform-libraries/service-starter` as a template).
1. Start infra: `docker compose -f infra/docker-compose.yml up --build -d`.

Local dev workflow (Phase 1)

- CI/CD: GitHub Actions for build/test and Helm-based deployment to K8s.
- Security: OIDC/JWT for auth; Keycloak as reference IdP. Secrets via Vault in production.
- Resilience: Client libraries should include timeouts, retries with jitter, circuit breakers, and bulkheads.
- Observability: OpenTelemetry traces, Prometheus metrics, Grafana dashboards.

Cross-cutting Concerns

- Infra: Kafka & Schema Registry, Postgres clusters per service, Redis for caching, Elasticsearch for search, Keycloak for auth (optional local instance).
- Platform services: API Gateway, Outbox processor, Feature Flags, Schema Registry extension, Observability (Prometheus/Grafana/Jaeger).
- Core domain services: Catalog, Pricing, Cart, Order, Payment, Inventory.

Core Components

- Deployment target: Kubernetes for staging/production; Docker Compose or Kind for local development.
- Data ownership: Each service owns its DB. Use the outbox pattern for reliable event publishing.
- Communication: REST (OpenAPI) for synchronous client-facing APIs; Kafka + Avro + Schema Registry for async domain events.
- Style: Microservices (26+ services) with clear domain boundaries.

Overview

Capture the core architecture, boundaries, and technology choices for the ecommerce platform to guide Phase 1 implementation.

Purpose


