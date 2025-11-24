# ADR 0003 — Event Format

Status: Proposed

Context

Events must be versioned and maintainable across many services and teams.

Decision

Use Avro for event schemas and Confluent Schema Registry for versioning and compatibility enforcement. Each event will have a lightweight envelope with metadata (traceId, timestamp, version).

Consequences

- Teams must publish Avro schemas to `docs/events/avro-schemas/`.
- Schema compatibility must be set to BACKWARD by default.

