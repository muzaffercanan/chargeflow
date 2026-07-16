# ChargeSquare Stage 1 Baseline and Stage 2 Requirements

This checklist keeps the completed mandatory Stage 1 acceptance criteria as the regression baseline and records the subsequently authorized Stage 2 bonus scope. Stretch goals remain excluded. References point to the case-study PDF sections that define each group.

## Scope and service boundaries

Reference: PDF Sections 3, 3.3, 3.4, and 3.10.

- [x] Deliver one correct `START -> STOP -> BILL -> SETTLE` flow.
- [x] Provide exactly two runtime services: Station Service and Session Service.
- [x] Keep wallet ownership inside Session Service as a wallet module.
- [x] Make Station Service the source of truth for stations, connectors, statuses, and tariffs.
- [x] Make Session Service the owner of sessions and the start/stop lifecycle.
- [x] Keep the required Session Service -> Station Service call as a real synchronous network REST call.
- [x] Complete the Stage 1 baseline without a Wallet Service, event broker, saga, service mesh, caching layer, rate limiting, or premature Stage 2 machinery.

## Data, persistence, and deterministic seed

Reference: PDF Sections 3.2, 3.5, 3.8, 3.9, and 3.11.

- [x] Use PostgreSQL as one shared database instance.
- [x] Keep Station Service tables in the `station` schema and Session Service/wallet tables in the `session` schema.
- [x] Create both schemas and all tables from a clean checkout with Flyway migrations.
- [x] Ensure data survives a container restart; core state must not live only in memory.
- [x] Seed one station deterministically.
- [x] Seed two connectors for the station, each linked to a tariff.
- [x] Seed connector `10` as `AVAILABLE` with tariff `8.50` TRY/kWh plus `2.00` TRY start fee.
- [x] Seed connector `11` as `AVAILABLE` with a documented tariff and connector type/power rating.
- [x] Seed user `7` with a deterministic wallet balance of `500.00` TRY.
- [x] Keep seed values consistent across migrations, README examples, and tests.
- [x] Use `BigDecimal` for energy, tariff values, wallet balances, and cost.
- [x] Round final cost to two decimal places with `RoundingMode.HALF_UP`.
- [x] Record session duration timestamps, but do not include duration in cost calculation.

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

- [x] Implement `POST /sessions` with required `userId` and `connectorId`.
  - [x] Validate both fields and return `400 Bad Request` with `VALIDATION_ERROR` when missing or invalid.
  - [x] Call `GET /connectors/{id}` on Station Service over the network.
  - [x] Return `404 Not Found` with `CONNECTOR_NOT_FOUND` when the connector is unknown.
  - [x] Return `409 Conflict` with `CONNECTOR_OCCUPIED` when the connector is not available.
  - [x] Create no session when validation or Station Service confirmation fails.
  - [x] Occupy the connector through Station Service.
  - [x] Create an `ACTIVE` session with an `Instant` start timestamp.
  - [x] Snapshot the tariff at start, including price per kWh, start fee, and currency.
  - [x] Return `201 Created` with the session id, user id, connector id, status, start timestamp, and tariff snapshot.
- [x] Implement `POST /sessions/{id}/stop` with required non-negative `energyKwh`.
  - [x] Return `400 Bad Request` with `VALIDATION_ERROR` when energy is missing or negative.
  - [x] Return `404 Not Found` with `SESSION_NOT_FOUND` for an unknown session.
  - [x] Return `409 Conflict` with `SESSION_NOT_ACTIVE` for a non-active session, including stop-twice.
  - [x] Guard the session before charging so a repeated stop cannot double-charge.
  - [x] Compute `energyKwh * pricePerKwh + startFee` from the tariff snapshot.
  - [x] Round the final cost to two decimals with `HALF_UP`.
  - [x] Debit the user's wallet and allow the balance to become negative.
  - [x] Persist the new wallet balance.
  - [x] Mark the session `COMPLETED`, persist the cost, and record an `Instant` end timestamp.
  - [x] Release the connector through Station Service.
  - [x] Return `200 OK` with receipt fields, cost, currency, and wallet balance after settlement.
- [x] Implement `GET /sessions/{id}`.
  - [x] Return `200 OK` with session status, cost, timestamps, and tariff snapshot where available.
  - [x] Return `404 Not Found` with `SESSION_NOT_FOUND` when unknown.
- [x] Implement `GET /users/{userId}/sessions`.
  - [x] Return `200 OK` with the user's session history.

## Error contract and dependency failures

Reference: PDF Sections 3.4, 3.5, 3.7, 3.9, and 3.10.

- [x] Use one small, consistent JSON error body everywhere, for example `{ "error": "CONNECTOR_OCCUPIED", "message": "Connector 10 is not AVAILABLE" }`.
- [x] Map expected validation, not-found, and conflict cases to `400`, `404`, and `409` respectively.
- [x] Fail fast with `503 Service Unavailable` and the same JSON error shape when Session Service cannot reach Station Service at start or stop.
- [x] Do not implement retries, fallbacks, brokers, sagas, or exactly-once machinery; document the trade-off instead.
- [x] Keep connector status consistent with session state on the normal start/stop flow.
- [x] Log session started, session stopped, cost charged, and wallet debited actions.

## Operations and configuration

Reference: PDF Sections 3.5, 3.6, 3.6.1, and 3.9.

- [x] Provide a health/readiness endpoint for each service, such as Actuator health endpoints.
- [x] Read database URL, credentials, ports, and downstream service URLs from environment/configuration; keep deterministic seed tariffs in Flyway.
- [x] Provide `.env.example` with placeholders only and no real secrets.
- [x] Commit no secrets or live `.env` files.
- [x] Make the full local flow runnable from a clean checkout with approximately one command.

## Tests

Reference: PDF Sections 3.9, 3.10, 6, and 9.2.

- [x] Add a worked cost-calculation test: `12.5` kWh at `8.50` plus `2.00` produces `108.25`.
- [x] Add a start-to-stop lifecycle test covering connector occupation, active session, completion, wallet debit, and connector release.
- [x] Add at least one invalid-case test, such as starting on an occupied connector.
- [x] Include persistence/integration coverage and verify the PostgreSQL-backed flow through Compose.
- [x] Keep tests intentional; coverage percentage is not a target.

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
- [x] Keep manifests plain YAML; attempt `kubectl apply --dry-run=client` and use offline schema validation when no API server is configured.

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
- [x] Record the human author's honest focused-hour total; it was not supplied during the final automated audit.
- [x] Add `DESIGN.md`, or an equivalent design section in the README, covering key decisions and unfinished work.
- [x] Explain decimal-safe money and final rounding.
- [x] Explain the wallet placement trade-off.
- [x] Explain tariff snapshotting and synchronous service communication.
- [x] Explain fail-fast dependency-down behavior.
- [x] Write a paragraph on idempotent retries and why the state guard prevents double-charge in the current scope.
- [x] Write a few sentences on stuck connectors/partial failure and possible recovery trade-offs.
- [x] State that retries/backoff, brokers, sagas, exactly-once delivery, scaling/HPA, ingress, service mesh, caching, rate limiting, and refresh tokens are not implemented.

## Stage 1 scope boundary

Reference: PDF Sections 3.10, 4, and 7.

- [x] Complete and verify Stage 1 before adding the separately authorized Stage 2 slice below.
- [x] Complete the Stage 1 baseline without wallet top-up, reservations, time-of-use tariffs, real idempotency keys, stuck-connector cleanup, domain events, a third Wallet Service, OpenAPI, or advanced observability before separately authorized later work.

## Stage 2 authentication and authorization

Reference: PDF Sections 4.1, 4.3, 4.3.1, 4.4, and 4.5.

- [x] Keep authentication as a small persisted module inside Session Service; add no third backend service.
- [x] Seed exactly one VIEWER and one ADMIN with BCrypt password hashes only.
- [x] Implement `POST /auth/login` with a generic 401 response for invalid or disabled users.
- [x] Issue short-lived HS256 JWTs with subject, role, issued-at, expiry, issuer, and audience.
- [x] Read signing secret, TTLs, issuer, audience, and allowed CORS origin from environment-backed configuration.
- [x] Keep health/probe and login endpoints public; require a valid JWT for domain APIs.
- [x] Require VIEWER or ADMIN for Station and Session reads.
- [x] Require ADMIN for human start/stop writes.
- [x] Require SERVICE or ADMIN for Station occupy/release operations.
- [x] Return consistent JSON `AUTHENTICATION_REQUIRED` and `ACCESS_DENIED` responses.
- [x] Generate independent short-lived SERVICE tokens in Session Service; never expose them to the browser.
- [x] Preserve bounded Station timeouts and the documented 503 mapping.
- [x] Allow only the configured CORS origin, required methods/headers, and no browser credentials.
- [x] Log successful/failed login and actor-aware start/stop actions without secrets or tokens.
- [x] Reference the Kubernetes signing secret via `secretKeyRef` without committing a Secret value.
- [x] Test login, invalid/expired tokens, anonymous access, both human roles, internal SERVICE access, and Stage 1 regression behavior.

## Stage 2 operations panel

Reference: PDF Sections 4.1, 4.2, 4.3.1, 4.4, and 4.5.

- [x] Add a React/TypeScript/Vite panel with login, connectors, sessions, and receipt/detail screens.
- [x] Store token, username, role, and expiry in `sessionStorage`; never store passwords.
- [x] Attach bearer tokens centrally, redirect anonymous users, and clear auth on 401 or expiry.
- [x] Treat role checks as UX only and retain backend authorization as authoritative.
- [x] Show connector status, power, type, price, start fee, and currency with loading/empty/error states.
- [x] Show seeded-user sessions and navigate each row to its receipt.
- [x] Show ADMIN the active-session stop form and VIEWER an explicit read-only state.
- [x] Validate required, non-negative energy with no more than six fractional digits client-side and server-side.
- [x] Handle 400, 401, 403, 404, 409, and 503 responses with understandable messages.
- [x] Use central relative API paths and Vite/Nginx reverse proxies.
- [x] Add a multi-stage panel image, SPA fallback, Compose service, and healthcheck.
- [x] Test login, redirect, VIEWER/ADMIN UX, 401/403, stop refresh, and loading/error states.
- [x] Document `sessionStorage` XSS risk and the production cookie/BFF versus CSRF trade-off.

## Stage 2 exclusions

- [x] Do not add refresh tokens, signup, password reset, OAuth login, token revocation, account lockout, a separate identity provider, API gateway, or database-backed browser sessions.
- [x] Do not add wallet top-up, reservations, richer tariffs, a Wallet Service, broker/event machinery, retry framework, or another Stage 1 stretch goal.

## Final authorized reviewability improvements

- [x] Add a substantive root `SECURITY.md` that matches the implemented auth, token storage, CORS, secrets, audit logging, and limitations.
- [x] Add Springdoc OpenAPI/Swagger to both existing services with Bearer security, role requirements, schemas, status codes, error contracts, and worked billing examples.
- [x] Keep local/demo documentation endpoints public without weakening domain endpoint authorization.
- [x] Add `scripts/e2e-smoke.sh` for the real authenticated Compose path using `curl` and `jq` without hardcoded generated session ids.
- [x] Add a separate CI E2E job with health waiting, failure logs, and unconditional project-scoped teardown; keep the original job intact.
- [x] Use focused `key=value` logs for required authentication, lifecycle, wallet, connector, authorization, and timeout events without sensitive values.
- [x] Reconcile README, DESIGN, decisions, and internal checklists without adding domain features.
- [x] Complete and record the final verification matrix after all changes.
