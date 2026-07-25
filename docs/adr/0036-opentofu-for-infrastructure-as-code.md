# ADR-0036: OpenTofu for Infrastructure as Code

## Status

Accepted

## Context

Block 8 provisions the production VM (ADR-0035) as code. Three tools
dominate the 2026 IaC landscape:

- **Terraform** (HashiCorp/IBM): the de-facto industry standard and the
  keyword used in job postings, including the Swiss financial sector.
  Licensed BSL 1.1 since 2023 — not OSI open source, though the
  restriction only affects vendors selling competing products.
- **OpenTofu** (Linux Foundation): the MPL-2.0 fork of Terraform 1.5.x.
  Same HCL language, same provider ecosystem (served through its own
  registry), same workflow. Since v1.7 it has genuinely diverged with
  features Terraform's CLI does not have: **client-side state and plan
  encryption** (AES-GCM, pluggable key providers: PBKDF2 passphrase, cloud
  KMS, OpenBao), provider `for_each` (v1.9), early variable evaluation in
  backend configuration (v1.8), `-exclude` flag.
- **Pulumi** (Apache 2.0 engine): infrastructure in general-purpose
  languages (TypeScript, Python, Go, Java, C#), bridging Terraform
  providers.

Decision criteria for this project: CV signal toward Swiss
banking/financial employers, learning transferability, licensing posture
of a public open-source portfolio project, and one project-specific
security fact — **the IaC state will contain secrets** (OpenStack
credentials, resource attributes) and the repository is public, so state
must live outside the repository and be protected wherever it lives.

## Decision

Pecunia uses **OpenTofu** as its IaC tool, with the OpenStack provider for
Infomaniak Public Cloud compute resources, and **client-side state
encryption enabled from the first `tofu init`** (PBKDF2 passphrase key
provider to start; the passphrase is held in the author's password manager
and in a GitHub Actions secret, alongside the SOPS age key custody defined
in ADR-0037).

The skill remains readable as "Terraform/OpenTofu" on the CV: HCL,
providers, plan/apply workflow, and state model are identical, and
migration in either direction remains a low-cost operation at the current
level of divergence.

## Consequences

### Positive

- **State encrypted before it leaves the machine** — defense in depth for
  a finance-themed public project; the backend never sees plaintext state.
  Terraform offers no equivalent in the CLI (backend-side at-rest
  encryption only).
- MPL-2.0 licensing is coherent with a public, open-source portfolio
  repository.
- 100 % skill transferability to Terraform; tutorials and provider
  documentation written for Terraform apply nearly verbatim.
- The licensing saga (BSL → fork → Linux Foundation) and the state
  encryption rationale are strong ecosystem-awareness interview material.

### Negative

- Job postings say "Terraform"; a one-line explanation ("OpenTofu is the
  Linux Foundation fork, same language and providers") may occasionally be
  needed.
- Growing divergence means occasional documentation gaps: an edge case
  documented for one tool may behave differently in the other.
- Losing the state-encryption passphrase makes the state unrecoverable
  (mitigated: state is reconstructible via `import` for a stack this
  small).

### Neutral

- Ephemeral values / write-only attributes (Terraform 1.10/1.11's
  alternative approach of keeping secrets out of state) are complementary
  concepts worth knowing for interviews.
- The ephemeral Azure demo (ADR-0035) can be driven by the same OpenTofu
  tooling with the `azurerm` provider.

## Alternatives Considered

### Terraform

Maximum keyword recognition and documentation surface. Rejected because
the practical differences all favor OpenTofu for this project (state
encryption, licensing coherence), while the CV cost is negligible —
recruiters read the skill as Terraform either way. BSL licensing is
legally irrelevant for this use but philosophically at odds with a public
open-source project.

### Pulumi

Attractive developer ergonomics for a Java/TypeScript author, and the
Exoscale/Azure ecosystems support it well. Rejected because HCL is the
lingua franca of infrastructure interviews in the target market, Pulumi's
footprint in Swiss financial job postings is small, its Java SDK is the
least mature of its SDKs, and switching later would be a rewrite (whereas
Terraform ↔ OpenTofu is nearly free).

## References

- OpenTofu state encryption: https://opentofu.org/docs/language/state/encryption/
- OpenTofu/Terraform divergence analyses (2026):
  https://scalr.com/learning-center/opentofu-vs-terraform ·
  https://encore.dev/articles/opentofu-vs-terraform-2026
- OpenStack provider on the OpenTofu registry:
  https://search.opentofu.org/provider/opentofu/openstack/latest
- ADR-0035 (hosting), ADR-0037 (secrets management)
