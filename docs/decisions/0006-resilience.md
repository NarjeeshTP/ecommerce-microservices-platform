# ADR 0006 — Resilience Strategy

Status: Proposed

Context

Distributed systems require defensive patterns to remain available and responsive under failure.

Decision

Adopt a resilience strategy including:
- Timeouts and request-level deadlines
- Retries with exponential backoff and jitter
- Circuit breakers at integration boundaries
- Bulkheads for isolating critical resources

Consequences

- Use a shared resilience library (`platform-libraries/resilience-config`) to standardize policies.

