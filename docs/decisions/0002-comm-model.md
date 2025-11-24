# ADR 0002 — Communication Model

Status: Proposed

Context

Microservices require both synchronous APIs for request/response flows and asynchronous messaging for integration and decoupling.

Decision

Adopt a hybrid communication model:
- Synchronous: REST (OpenAPI) for request/response client-facing endpoints.
- Asynchronous: Kafka for domain events and integration across services.

Consequences

- Services will expose OpenAPI specs.
- Events will be schema-managed using Avro and Schema Registry.

