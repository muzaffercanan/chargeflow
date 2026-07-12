# ChargeSquare Stage 1 Case Requirements

This checklist converts the mandatory Stage 1 requirements and acceptance criteria in the case study into reviewable work items. Stage 2 and stretch goals are explicitly excluded. References point to the case-study PDF sections that define each group.

## Scope and service boundaries

Reference: PDF Sections 3, 3.3, 3.4, and 3.10.

- [ ] Deliver one correct `START -> STOP -> BILL -> SETTLE` flow.
- [ ] Provide exactly two runtime services: Station Service and Session Service.
- [ ] Keep wallet ownership inside Session Service as a wallet module.
- [ ] Make Station Service the source of truth for stations, connectors, statuses, and tariffs.
- [ ] Make Session Service the owner of sessions and the start/stop lifecycle.
- [ ] Keep the required Session Service -> Station Service call as a real synchronous network REST call.
- [ ] Do not add a Wallet Service, event broker, saga, service mesh, caching layer, rate limiting, authentication, or other Stage 2/stretch machinery.

## Data, persistence, and deterministic seed

Reference: PDF Sections 3.2, 3.5, 3.8, 3.9, and 3.11.

- [ ] Use PostgreSQL as one shared database instance.
- [ ] Keep Station Service tables in the `station` schema and Session Service/wallet tables in the `session` schema.
- [ ] Create both schemas and all tables from a clean checkout with Flyway migrations.
- [ ] Ensure data survives a container restart; core state must not live only in memory.
- [x] Seed one station deterministically.
- [x] Seed two connectors for the station, each linked to a tariff.
- [x] Seed connector `10` as `AVAILABLE` with tariff `8.50` TRY/kWh plus `2.00` TRY start fee.
- [x] Seed connector `11` as `AVAILABLE` with a documented tariff and connector type/power rating.
- [ ] Seed user `7` with a deterministic wallet balance of `500.00` TRY.
- [ ] Keep seed values consistent across migrations, README examples, and tests.
- [ ] Use `BigDecimal` for energy, tariff values, wallet balances, and cost.
- [ ] Round final cost to two decimal places with `RoundingMode.HALF_UP`.
- [ ] Record session duration timestamps, but do not include duration in cost calculation.

## Station Service API

Reference: PDF Sections 3.2.1, 3.3, 3.7.1, 3.7.2, 3.7.6, and 3.9.

- [x] Implement `GET /connectors/{id}`.
  - [x] Return `200 OK` with connector id, station id, type, power rating, status, and tariff (`tariffId`, `pricePerKwh`, `startFee`, `currency`).
  - [x] Return `404 Not Found` with the consistent JSON error body and `CONNECTOR_NOT_FOUND` for an unknown connector.
- [x] Implement `GET /stations/{id}/connectors`.
  - [x] Return `200 OK` with the station's connector array, current statuses, and tariffs.
  - [x] Return `404 Not Found` with `STATION_NOT_FOUND` and the same error shape for an unknown station.
- [x] Implement internal `POST /connectors/{id}/occupy`.
  - [x] Return `200 OK` and `OCCUPIED` when an available connector is occupied.
  - [x] Return `404 Not Found` with `CONNECTOR_NOT_FOUND` for an unknown connector.
  - [x] Return `409 Conflict` with `CONNECTOR_OCCUPIED` when it is already occupied.
- [x] Implement internal `POST /connectors/{id}/release`.
  - [x] Return `200 OK` and `AVAILABLE` when an occupied connector is released.
  - [x] Return `404 Not Found` with `CONNECTOR_NOT_FOUND` when the connector is unknown; release is idempotent for an existing available connector.
- [x] Keep connector state transitions guarded and persisted.

## Session Service API and lifecycle

Reference: PDF Sections 3.2, 3.7.3, 3.7.4, 3.7.5, 3.8, and 3.9.

- [ ] Implement `POST /sessions` with required `userId` and `connectorId`.
  - [ ] Validate both fields and return `400 Bad Request` with `VALIDATION_ERROR` when missing or invalid.
  - [ ] Call `GET /connectors/{id}` on Station Service over the network.
  - [ ] Return `404 Not Found` with `CONNECTOR_NOT_FOUND` when the connector is unknown.
  - [ ] Return `409 Conflict` with `CONNECTOR_OCCUPIED` when the connector is not available.
  - [ ] Create no session when validation or Station Service confirmation fails.
  - [ ] Occupy the connector through Station Service.
  - [ ] Create an `ACTIVE` session with an `Instant` start timestamp.
  - [ ] Snapshot the tariff at start, including price per kWh, start fee, and currency.
  - [ ] Return `201 Created` with the session id, user id, connector id, status, start timestamp, and tariff snapshot.
- [ ] Implement `POST /sessions/{id}/stop` with required non-negative `energyKwh`.
  - [ ] Return `400 Bad Request` with `VALIDATION_ERROR` when energy is missing or negative.
  - [ ] Return `404 Not Found` with `SESSION_NOT_FOUND` for an unknown session.
  - [ ] Return `409 Conflict` with `SESSION_NOT_ACTIVE` for a non-active session, including stop-twice.
  - [ ] Guard the session before charging so a repeated stop cannot double-charge.
  - [ ] Compute `energyKwh * pricePerKwh + startFee` from the tariff snapshot.
  - [ ] Round the final cost to two decimals with `HALF_UP`.
  - [ ] Debit the user's wallet and allow the balance to become negative.
  - [ ] Persist the new wallet balance.
  - [ ] Mark the session `COMPLETED`, persist the cost, and record an `Instant` end timestamp.
  - [ ] Release the connector through Station Service.
  - [ ] Return `200 OK` with receipt fields, cost, currency, and wallet balance after settlement.
- [ ] Implement `GET /sessions/{id}`.
  - [ ] Return `200 OK` with session status, cost, timestamps, and tariff snapshot where available.
  - [ ] Return `404 Not Found` with `SESSION_NOT_FOUND` when unknown.
- [ ] Implement `GET /users/{userId}/sessions`.
  - [ ] Return `200 OK` with the user's session history.

## Error contract and dependency failures

Reference: PDF Sections 3.4, 3.5, 3.7, 3.9, and 3.10.

- [ ] Use one small, consistent JSON error body everywhere, for example `{ "error": "CONNECTOR_OCCUPIED", "message": "Connector 10 is not AVAILABLE" }`.
- [ ] Map expected validation, not-found, and conflict cases to `400`, `404`, and `409` respectively.
- [ ] Fail fast with `503 Service Unavailable` and the same JSON error shape when Session Service cannot reach Station Service at start or stop.
- [ ] Do not implement retries, fallbacks, brokers, sagas, or exactly-once machinery; document the trade-off instead.
- [ ] Keep connector status consistent with session state on the normal start/stop flow.
- [ ] Log session started, session stopped, cost charged, and wallet debited actions.

## Operations and configuration

Reference: PDF Sections 3.5, 3.6, 3.6.1, and 3.9.

- [ ] Provide a health/readiness endpoint for each service, such as Actuator health endpoints.
- [ ] Read database URL, credentials, ports, downstream service URLs, and tariff defaults from environment/configuration.
- [ ] Provide `.env.example` with placeholders only and no real secrets.
- [ ] Commit no secrets or live `.env` files.
- [ ] Make the full local flow runnable from a clean checkout with approximately one command.

## Tests

Reference: PDF Sections 3.9, 3.10, 6, and 9.2.

- [ ] Add a worked cost-calculation test: `12.5` kWh at `8.50` plus `2.00` produces `108.25`.
- [ ] Add a start-to-stop lifecycle test covering connector occupation, active session, completion, wallet debit, and connector release.
- [ ] Add at least one invalid-case test, such as starting on an occupied connector.
- [ ] Include persistence/integration coverage sufficient to prove the real database-backed flow.
- [ ] Keep tests intentional; coverage percentage is not a target.

## Docker and Compose

Reference: PDF Sections 3.6, 3.6.1, 3.9, and 3.12.

- [x] Add `station-service/Dockerfile`.
- [x] Add `session-service/Dockerfile`.
- [x] Add root `docker-compose.yml` that starts PostgreSQL, Station Service, and Session Service.
- [x] Wire database settings and service URLs through Compose environment variables.
- [x] Verify `docker compose up` from a clean checkout brings up the complete flow.

## Kubernetes

Reference: PDF Sections 3.6, 3.6.1, 3.9, and 3.12.

- [x] Add `k8s/station-deployment.yaml` and `k8s/station-service.yaml`.
- [x] Add `k8s/session-deployment.yaml` and `k8s/session-service.yaml`.
- [x] Add `k8s/configmap.yaml` with real configuration consumed by at least one Deployment.
- [ ] Keep manifests plain YAML and validate with `kubectl apply --dry-run=client` where a cluster is unavailable.

## CI

Reference: PDF Sections 3.6, 3.6.1, 3.9, and 3.12.

- [x] Add `.github/workflows/ci.yml` triggered on push.
- [x] Build the Maven project.
- [x] Run the automated tests.
- [x] Build the Station Service and Session Service Docker images.
- [x] Do not push images or deploy from this workflow.

## README and design documentation

Reference: PDF Sections 3.10, 3.11, 3.12, 5, 6, 8.2, and 9.2.

- [x] Add a top-level `README.md` with the one-command run path.
- [x] Add two or three sample requests showing the end-to-end start and stop flow.
- [x] Document the main endpoints.
- [x] Document the Java/Spring/PostgreSQL stack and why it was chosen.
- [x] Document how to run tests.
- [x] Document assumptions, including simulated meter energy and the negative-wallet policy.
- [x] Include an honest time-spent and “what I would do next” note.
- [x] Add `DESIGN.md`, or an equivalent design section in the README, covering key decisions and unfinished work.
- [x] Explain decimal-safe money and final rounding.
- [x] Explain the wallet placement trade-off.
- [x] Explain tariff snapshotting and synchronous service communication.
- [x] Explain fail-fast dependency-down behavior.
- [x] Write a paragraph on idempotent retries and why the state guard prevents double-charge in the current scope.
- [x] Write a few sentences on stuck connectors/partial failure and possible recovery trade-offs.
- [x] State that retries/backoff, brokers, sagas, exactly-once delivery, scaling/HPA, ingress, service mesh, caching, rate limiting, and refresh tokens are not implemented.

## Explicitly out of scope

Reference: PDF Sections 3.10, 4, and 7.

- [ ] Do not implement Stage 2 admin UI, authentication, JWT, RBAC, security hardening, or `SECURITY.md` requirements.
- [ ] Do not implement stretch goals such as wallet top-up, reservations, time-of-use tariffs, real idempotency keys, stuck-connector cleanup, domain events, a third Wallet Service, or OpenAPI/advanced observability.
