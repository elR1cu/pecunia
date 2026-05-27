# ADR-0005: Schema-first API Design with OpenAPI

## Status

Accepted

## Context

The HTTP API between the Angular frontend and the Spring Boot backend must
be defined, documented, and kept synchronized between both sides. Two
opposing approaches exist:

1. **Code-first**: backend developers write Spring controller annotations,
   from which an OpenAPI specification is auto-generated (typically via
   Springdoc). The frontend either consumes this generated spec or writes
   its own client manually.
2. **Schema-first** (contract-first): the OpenAPI specification is hand-
   written as the source of truth. Both backend and frontend generate code
   from it: controller interfaces and DTOs for the backend, typed clients
   for the frontend.

The choice affects API consistency, contract stability, and the development
workflow.

## Decision

Pecunia uses **schema-first API design** with OpenAPI 3.1.

The specification lives in `contracts/openapi.yaml` and is the single source
of truth for the HTTP API.

### Workflow

1. The author edits `contracts/openapi.yaml` to define or modify endpoints.
2. The backend Maven build invokes `openapi-generator-maven-plugin` with the
   `spring` generator to produce:
   - Controller interfaces (Spring-annotated, ready to implement).
   - DTO records.
3. The frontend npm build invokes `@openapitools/openapi-generator-cli` with
   the `typescript-angular` generator to produce:
   - A typed Angular service per resource.
   - TypeScript interfaces matching the DTOs.
4. The author implements the controller interfaces with business logic
   delegating to use cases.
5. Springdoc exposes Swagger UI at `/swagger-ui.html`, reflecting the
   contract (not introspecting the code).

## Consequences

### Positive

- **Single source of truth**: any drift between backend and frontend is
  impossible — both generate from the same file.
- **Contract stability**: the API design is deliberate, not emergent. The
  author thinks about the contract before implementing it.
- **Parallel development**: backend and frontend can progress independently
  once the contract is agreed.
- **Type safety on both sides**: generated DTOs in Java and TypeScript
  prevent mismatches at compile time.
- **Documentation by default**: the specification is the documentation.
- **Industry standard**: schema-first is the norm in serious API-driven
  organizations.

### Negative

- **Initial overhead**: writing OpenAPI YAML by hand requires familiarity
  with the specification.
- **Build complexity**: code generation is part of the build pipeline; IDE
  configuration may need adjustment to recognize generated sources.
- **Learning curve**: the OpenAPI generator has many options and quirks.

### Neutral

- **Generated code is not committed**: it lives in `target/generated-sources`
  and `src/app/api/generated`, recreated on each build.

## Alternatives Considered

### Code-first with Springdoc

Rejected because:
- The OpenAPI specification becomes a side-effect of the code, not a
  deliberate contract.
- Frontend developers must consume a spec that may change with each backend
  refactor.
- API design tends to leak implementation details (Spring annotations
  influence the spec).
- Less impressive demonstration of senior practices.

### No specification, hand-written clients

Rejected because:
- Drift is inevitable.
- Type safety is lost.
- Documentation becomes a manual chore.

## References

- OpenAPI Specification: https://spec.openapis.org/oas/v3.1.0
- OpenAPI Generator: https://openapi-generator.tech/
- "API-First vs Code-First": https://swagger.io/blog/api-design/design-first-or-code-first-api-development/
