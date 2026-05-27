# ADR-0010: k3d Local Kubernetes Instead of AWS for Cloud-Native Demo

## Status

Accepted

## Context

Pecunia is a portfolio project intended to demonstrate cloud-native
engineering practices for Senior Backend Java positions. Cloud-native
deployment typically implies a major cloud provider (AWS, GCP, Azure) with
managed Kubernetes (EKS, GKE, AKS), Infrastructure as Code (Terraform), and
production-grade observability.

The challenge: maintaining a continuously deployed AWS environment on a
small budget is expensive. Pricing estimates for a minimal EKS-based
deployment exceed $100/month even with aggressive optimization. Sustaining
this for the duration of a job search is not feasible.

Two options:

1. **AWS deployment, kept always-on**: prohibitive cost.
2. **AWS deployment, spun up only for interviews**: feasible but adds risk
   (provisioning failures during critical moments) and requires constant
   familiarity refresh.
3. **k3d local Kubernetes**: a full Kubernetes cluster runs locally in
   Docker, replicating the architecture without cloud cost.

## Decision

Pecunia uses **k3d (k3s in Docker)** as the local Kubernetes demonstration
environment. The application is deployed to k3d using Helm charts, with
Ingress, secrets management, persistent volumes, liveness/readiness probes,
resource limits, and Horizontal Pod Autoscaling configured.

The k3d environment is **explicitly documented as the cloud-native demo**,
with rationale clearly stated:

> "This project demonstrates Kubernetes-based deployment in a local k3d
> cluster, mirroring the patterns used in production on AWS EKS, GCP GKE,
> or Azure AKS. The Helm charts, manifests, probes, and configurations are
> portable; only the cluster substrate differs."

AWS deployment is **not in scope** for the MVP. It may be revisited
post-employment when budget allows or as a focused mini-project on AWS
specifics.

## Consequences

### Positive

- **Zero ongoing infrastructure cost**: k3d runs on the author's laptop.
- **Equivalent technical demonstration**: Helm, Ingress, Pod autoscaling,
  observability — all the cloud-native patterns work identically in k3d.
- **Faster iteration**: no waiting for cloud resources to provision.
- **Interview-friendly**: the entire environment can be brought up in
  minutes on a laptop during a live technical discussion.
- **Discipline signal**: senior engineers recognize the choice as
  pragmatic, not as a limitation. Explaining the rationale in interviews
  demonstrates cost-awareness.

### Negative

- **No cloud-provider-specific experience demonstrated**: the project does
  not show familiarity with IAM, VPC, RDS, Secrets Manager, or
  cloud-specific networking. This may be a gap for AWS-specific roles.
- **No load testing against real-world latencies**: k3d is single-host;
  cross-AZ failures cannot be simulated.

### Neutral

- **Helm charts and manifests are portable**: migration to a cloud-managed
  Kubernetes service would primarily involve adjusting cluster-specific
  configurations (storage class, ingress controller, secrets backend).

## Alternatives Considered

### AWS EKS, always on

Rejected for cost. ~$100/month sustained is incompatible with the project
budget.

### AWS EKS, spun up for interviews

Considered but rejected because:
- Provisioning during a high-stress moment (live interview) introduces
  failure risk.
- Maintaining familiarity with the environment requires regular practice,
  which means regular spin-ups, which means recurring cost anyway.
- A static local environment is more reliable and demonstrable.

### Simpler deployment without Kubernetes (e.g., Docker Compose on VPS only)

This is the chosen production environment (see ADR for VPS deployment).
However, this alone does not demonstrate Kubernetes proficiency. The k3d
environment is additive: production runs on Docker Compose for cost,
demonstration runs on k3d for engineering credibility.

## References

- k3d documentation: https://k3d.io/
- k3s documentation: https://docs.k3s.io/
- "k3d vs kind vs minikube" comparisons (community articles).
