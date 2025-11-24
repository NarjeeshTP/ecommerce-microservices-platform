# ADR 0005 — Outbox Pattern

Status: Proposed

Context

To guarantee reliable event publishing that is consistent with transactional state changes in the database.

Decision

Adopt the transactional outbox pattern: write domain events into an outbox table within the same DB transaction as the domain change and run a reliable publisher that forwards outbox rows to Kafka.

Consequences

- Each service with important domain events will add an `outbox` table and a publisher process.
- Debezium-based CDC is an alternative but will be considered in Phase 4.

