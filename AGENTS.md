# ChargeSquare Agent Guidance

## Source of truth

Before changing the repository, read these files in order:

1. `docs/CASE_REQUIREMENTS.md` - the mandatory Stage 1 checklist.
2. `docs/DECISIONS.md` - the locked technical choices and non-goals.
3. `docs/TASKS.md` - the implementation status and remaining work.

If another instruction conflicts with the requirements or decisions, stop and resolve the conflict explicitly. Do not silently expand scope.

## Scope restrictions

- This repository contains only Station Service and Session Service.
- Wallet remains a module inside Session Service.
- Do not add Stage 2, authentication, authorization, or stretch goals.
- Do not add business endpoints, domain entities, controllers, or business logic during the foundation phase.
- Do not add a third service, broker, saga, cache, service mesh, retry framework, or cross-service table access.
- Each service may access only its own PostgreSQL schema; cross-service data is accessed through the owning service's REST API.

## Coding conventions

- Use Java 21, Spring Boot 3.5.16, and Maven.
- Prefer small functions with one clear responsibility and descriptive names.
- Use constructor injection; do not use field injection.
- Use `Instant` for persisted timestamps and API timestamps.
- Use `BigDecimal` for energy, tariffs, balances, and cost; round final cost with `HALF_UP` to two decimals.
- Use a consistent JSON API error body with a stable error code and human-readable message.
- Fail fast with the documented `503 Service Unavailable` response for unavailable downstream dependencies.
- Read ports, database settings, service URLs, and other operational configuration from environment-backed configuration.
- Keep migrations deterministic and keep seed values consistent with the README and tests.
- Add focused tests for behavior and invalid states; do not chase coverage for its own sake.
- Keep changes small and update `docs/TASKS.md` only when the corresponding artifact or behavior actually exists.
