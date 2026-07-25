# ADR-0037: SOPS + age for Production Secrets Management

## Status

Accepted

## Context

The production deployment (ADR-0035) needs a secrets strategy before the
first deployment, because retrofitting one means migrating secrets already
in production. Three classes of secrets exist:

1. **IaC-time**: OpenStack API credentials, OpenTofu state-encryption
   passphrase — used locally and by CI.
2. **CI/CD-time**: deployment SSH key, container registry credentials.
3. **Runtime on the VM**: PostgreSQL passwords, Keycloak admin and client
   secrets, and every value the production Docker Compose stack reads from
   its environment.

Classes 1 and 2 have a standard answer (author's password manager +
GitHub Actions Secrets) and are not the subject of this ADR. The open
question is class 3: where is the *source of truth* for runtime secrets,
given that the repository is public, the CLAUDE.md security baseline
mandates "all secrets via environment variables, never committed to Git",
and the recoverability doctrine (ADR-0035) requires the VM to be fully
reconstructible from the repository plus a small, well-defined set of
keys.

## Decision

Runtime secrets are stored **encrypted in the repository with SOPS**
(CNCF), using **age** as the key provider, in dotenv format:

- `deploy/secrets/production.enc.env` is committed; values are encrypted
  (AES-256-GCM, per-value, with a file-integrity MAC), variable *names*
  remain readable so Git diffs show *which* secret changed, never its
  value.
- A `.sops.yaml` creation rule pins the age recipient for
  `deploy/secrets/*.enc.env`.
- The age private key exists in exactly two places: the author's password
  manager and a GitHub Actions secret (`SOPS_AGE_KEY`).
- The deployment job decrypts to a transient `.env`, ships it to the VM
  (`chmod 600`), and the stack consumes it as environment variables —
  the CLAUDE.md rule is unchanged in mechanism (env vars remain the
  injection vehicle); the *source of truth* moves into versioned,
  encrypted files.
- Guardrails: `.gitignore` excludes `*.env` while allowing `*.enc.env`;
  plaintext files never exist in the working tree except transiently;
  a secret once committed encrypted is never "un-committed" — if the age
  key is ever compromised, the *secrets themselves* are rotated, since
  old encrypted versions remain in public Git history.

A full secret manager (Vault/OpenBao) is **explicitly deferred** with a
documented adoption trigger (see roadmap "Technology Adoption Triggers"):
automatic/dynamic rotation needs, a second secrets consumer (e.g.,
Kubernetes at Block 10 via external-secrets), or a dedicated post-MVP
learning exercise. OpenBao would be the coherent choice given ADR-0036.

## Consequences

### Positive

- Versioned, auditable source of truth: Git history *is* the rotation log;
  rotation is `sops edit` + commit + redeploy.
- The public repository stays publishable: only ciphertext is committed.
- VM reconstruction (RTO < 1 h, ADR-0035) needs only the repository and
  two keys (age, state passphrase) from the password manager.
- Lightweight: ~2–3 h setup, no server component, no new runtime service —
  consistent with the accepted-SPOF doctrine.
- Tells the same *reasoning* story as Vault (encryption, custody, rotation,
  least exposure) calibrated to actual scale — good interview material.

### Negative

- The age private key is a single point of trust: loss makes secrets
  unrecoverable (mitigated by password-manager custody); compromise forces
  rotation of all secrets, not just the key.
- One more tool and one discipline to maintain (never commit plaintext);
  a pre-commit secret scanner (e.g., gitleaks) is a recommended follow-up,
  coherent with the existing Trivy supply-chain baseline.
- Secret *names* and stack composition are visible in the repository —
  acceptable here (the stack is public by design), but a real constraint
  in contexts where secret existence is itself sensitive.

### Neutral

- Naming convention `production.enc.env` leaves room for
  `staging.enc.env` with different recipients if a second environment
  appears.
- age can encrypt to existing SSH ed25519 keys; a dedicated age key is
  used anyway to decouple SSH identity from secrets access.

## Alternatives Considered

### Plain `.env` on the VM, outside Git

The classic single-VPS answer: provision once, `chmod 600`. Rejected
because it leaves no versioned source of truth (rebuilding the VM depends
on an unmanaged file), no traceable rotation, and contradicts the
"reconstructible from the repository" recoverability goal.

### Ansible Vault

Encrypts secrets in the Ansible provisioning flow already planned for
Block 8. Rejected as the *primary* mechanism because it couples secret
delivery to provisioning runs (application-only redeployments would not
carry secret changes), and SOPS offers better ergonomics (per-value
encryption, readable diffs, `sops edit`) and stronger key options.
Ansible may still consume the SOPS-decrypted output during provisioning.

### HashiCorp Vault / OpenBao server

The full secret-manager answer: API, leases, dynamic rotation, audit log,
and a first-class CV keyword in Swiss banking. Rejected *for the MVP* as
objective over-engineering: it adds a service to operate on the same VM it
protects (unseal/bootstrap chicken-and-egg at boot), RAM and attack
surface, while its differentiators (dynamic rotation, many consumers)
have no user at this scale. Deferred with an explicit adoption trigger.

## References

- SOPS: https://github.com/getsops/sops
- age: https://github.com/FiloSottile/age
- ADR-0035 (recoverability doctrine), ADR-0036 (OpenTofu state
  encryption, key custody)
