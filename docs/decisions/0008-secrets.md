# ADR 0008 — Secrets Management

Status: Proposed

Context

Secrets must be stored securely and rotated regularly.

Decision

For development: use sealed secrets or local environment variables. For production: use HashiCorp Vault (or cloud provider secret manager) with Kubernetes integration.

Consequences

- Secrets will not be stored in the repository.
- Developers will have a documented local dev secret provisioning flow in `infra/`.

