# ADR 0001 — Use Kubernetes
- Operational complexity increases but enables cloud portability and standard infra automation.
- Helm charts or K8s manifests will be maintained for each service.
- Teams must package services as container images.

Consequences

We will adopt Kubernetes as the primary deployment platform for production and staging. For local development we will support Docker Compose and Kind/Minikube for fast iteration.

Decision

We need a primary deployment target for the platform that supports containerized workloads, service discovery, network policies, autoscaling, and standard cloud-native tooling.

Context

Status: Proposed


