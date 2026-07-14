# ChargeSquare Foundation Decisions

These decisions are locked for the Stage 1 implementation. `CASE_REQUIREMENTS.md` is the executable checklist; this file explains the chosen direction and its trade-offs.

## Locked technical decisions

| Decision | Choice | Justification |
| --- | --- | --- |
| Language | Java 21 | It is the requested runtime and gives the team a current LTS Java baseline. |
| Framework | Spring Boot 3.5.16 | It is the requested house-stack version and supplies the web, validation, persistence, actuator, and test integration needed by the case. |
| Repository | Maven multi-module monorepo | One checkout keeps the two services and shared infrastructure easy to build and review together. |
| Runtime services | Station Service and Session Service only | Two services cover the essential flow without adding an unnecessary network hop. |
| Wallet placement | Wallet is a module inside Session Service | Settlement is part of the session stop transaction, so folding it in keeps the essential slice small and cohesive. |
| Database | One PostgreSQL instance with `station` and `session` schemas | A shared container is simple to run while schema ownership keeps service data boundaries explicit. |
| Persistence setup | Flyway migrations with deterministic seed data | Versioned migrations make a clean checkout reproducible and seed values make the walkthrough and tests stable. |
| Service communication | Synchronous REST from Session Service to Station Service | The case requires a real network call and synchronous REST is the smallest clear implementation for this flow. |
| Tariff behavior | Snapshot the tariff when a session starts | A completed session must be priced using the agreed tariff even if the station tariff changes later. |
| Numeric representation | `BigDecimal` for energy, tariffs, balances, and cost | Decimal arithmetic avoids binary floating-point drift in the billing path. |
| Cost rounding | Final cost rounded to two decimals with `HALF_UP` | This matches the case requirement and makes the billing rule explicit and deterministic. |
| Insufficient balance | Allow the wallet balance to become negative on stop | The stop operation remains truthful about energy delivered; the case explicitly permits this policy when documented. |
| Dependency failure | Bound connection/response waits and fail with a consistent `503 Service Unavailable` JSON error | Callers receive a clear transient dependency error without hidden retries; a release timeout rolls back the local stop transaction. |

## Deterministic baseline data

The implementation will use these values consistently in migrations, README examples, and tests:

- Station `1`: `ChargeSquare Demo Station`.
- Connector `10`: `CCS2-DC`, `60` kW, `AVAILABLE`, tariff `5`, `8.50` TRY/kWh, `2.00` TRY start fee.
- Connector `11`: `Type2-AC`, `22` kW, `AVAILABLE`, tariff `5`, `8.50` TRY/kWh, `2.00` TRY start fee.
- User `7`: wallet balance `500.00` TRY.

The worked case remains: `12.5` kWh on connector `10` costs `108.25` TRY, leaving `391.75` TRY.

## Explicit non-goals

- No Stage 2 admin panel, authentication, JWT, RBAC, or security implementation.
- No stretch goals: top-up, reservations, time-of-use pricing, real idempotency keys, stuck-connector cleanup, domain events, a separate Wallet Service, OpenAPI, or advanced observability.
- No caching, rate limiting, service mesh, retries, backoff, fallback, broker, saga, exactly-once delivery, HPA, ingress, or refresh-token machinery.
- No cross-service table access; each service uses only its own schema and the Station Service API for station data.
