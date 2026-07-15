# ChargeSquare Implementation Tasks

Status is intentionally conservative: an item is checked only when the corresponding artifact or behavior exists in the repository.

## Foundation

- [x] Create the Maven parent project.
- [x] Create the `station-service` module.
- [x] Create the `session-service` module.
- [x] Add empty Spring Boot application entry points.
- [x] Add the foundation dependencies allowed by the case.
- [x] Add Maven Wrapper pinned to Apache Maven 3.9.11.
- [x] Add environment-driven service ports and actuator health configuration.
- [x] Verify both empty services start and report healthy through Actuator.
- [x] Add PostgreSQL connection configuration for both owned schemas.
- [x] Add Flyway migration locations and schema history configuration.
- [x] Add deterministic station, connector, tariff, user, and wallet seed migrations.
- [x] Add Station Service PostgreSQL configuration and Flyway migrations for the `station` schema.

## Station Service

- [x] Model stations, connectors, and tariffs in the `station` schema.
- [x] Implement connector and station connector-list reads.
- [x] Implement guarded internal occupy/release operations.
- [x] Add Station Service validation and error mapping.
- [x] Add Station Service action logging.

## Session Service

- [x] Model sessions and the wallet module in the `session` schema.
- [x] Implement the synchronous Station Service client.
- [x] Implement start validation, tariff snapshot, occupy call, and `ACTIVE` session creation.
- [x] Implement stop validation, cost calculation, wallet debit, completion, and release call.
- [x] Implement session and user-session reads.
- [x] Add consistent API errors, including fail-fast `503` dependency failures.
- [x] Add lifecycle and billing action logging.

## Tests

- [x] Add the worked `12.5 * 8.50 + 2.00 = 108.25` cost test.
- [x] Add a start-to-stop lifecycle test.
- [x] Add an invalid-case test.
- [x] Add persistence/integration coverage for the real database-backed flow.
- [x] Add Station Service endpoint and atomic-transition tests.
- [x] Add real HTTP response-timeout tests for start and stop rollback behavior.
- [x] Reject energy values that exceed the persisted six-digit fractional precision.

## Docker

- [x] Add `station-service/Dockerfile`.
- [x] Add `session-service/Dockerfile`.
- [x] Add root `docker-compose.yml` with PostgreSQL and both services.
- [x] Verify the documented one-command Compose flow from a clean checkout.

## Kubernetes/CI

- [x] Add Station Service Deployment and Service manifests.
- [x] Add Session Service Deployment and Service manifests.
- [x] Add a ConfigMap consumed by a Deployment.
- [x] Attempt `kubectl apply --dry-run=client` and document the unavailable Kubernetes API server.
- [x] Add push-triggered CI to build, test, and build both images.

## Documentation

- [x] Convert the mandatory Stage 1 requirements into `CASE_REQUIREMENTS.md` checkboxes.
- [x] Record locked choices and explicit non-goals in `DECISIONS.md`.
- [x] Create this implementation task list.
- [x] Add repository guidance in `AGENTS.md`.
- [x] Add short `CLAUDE.md` and `GEMINI.md` agent entry points.
- [x] Add the top-level README with run instructions, examples, assumptions, tests, and next steps.
- [x] Add the short design note covering the two required reasoning paragraphs.
- [x] Place the final design note at root `DESIGN.md` and document bounded dependency timeouts.

## Final Audit

- [x] Confirm every checked requirement in `CASE_REQUIREMENTS.md` is implemented and tested.
- [x] Confirm the Stage 1 baseline contained no premature Stage 2 or stretch-goal artifacts before Stage 2 began.
- [x] Confirm no secrets or live `.env` files are committed.
- [x] Confirm no cross-service table access exists.
- [x] Run the root Maven test command.
- [x] Run the documented Compose flow.
- [x] Attempt kubectl client dry-run and validate all five Kubernetes resources offline with kubeconform.
- [x] Review README examples against the actual API responses.

## Stage 2 Backend Security

- [x] Add persisted BCrypt-only VIEWER and ADMIN demo users inside Session Service.
- [x] Implement `POST /auth/login` with short-lived environment-configured JWTs.
- [x] Validate signature, issuer, audience, and expiry in both backend services.
- [x] Enforce VIEWER/ADMIN reads, ADMIN session writes, and SERVICE/ADMIN Station transitions.
- [x] Return the consistent JSON API error body for 401 and 403 responses.
- [x] Generate a separate short-lived SERVICE JWT for occupy/release calls.
- [x] Add explicit single-origin CORS configuration without credentials.
- [x] Add actor/role security logs without passwords, JWTs, secrets, or headers.
- [x] Add focused authentication, expiry, authorization, and regression tests.

## Stage 2 Panel

- [x] Add the React/TypeScript/Vite login screen.
- [x] Add stations/connectors, sessions, and receipt/detail screens.
- [x] Add ADMIN stop-session UX and VIEWER read-only UX.
- [x] Persist the demo tab session in `sessionStorage` and handle expiry/401 logout.
- [x] Add a centralized API client with 400/401/403/404/409/503 messages.
- [x] Add focused Vitest/React Testing Library coverage.
- [x] Add the panel Nginx image, SPA fallback, API proxies, and healthcheck.
- [x] Add the panel to Compose and CI.

## Stage 2 Verification

- [x] Run frontend tests, production build, dependency audit, panel image build, and Compose config.
- [x] Run complete Maven test and verify after all changes.
- [x] Build both backend images after all changes.
- [x] Run a clean four-container Compose startup and live VIEWER/ADMIN/internal-service flow.
- [x] Verify SPA direct-route refresh, dependency timeout regression, and persistence restart.
- [x] Complete tracked-secret and final diff inspection.

## Final Reviewability Improvements

- [x] Add root `SECURITY.md` covering implemented controls, trade-offs, audit events, and honest limitations.
- [x] Add compatible Springdoc OpenAPI/Swagger dependencies and documentation to both services.
- [x] Permit local/demo OpenAPI endpoints while preserving JWT authorization on domain operations.
- [x] Add the authenticated Docker Compose E2E smoke script with robust `jq` assertions.
- [x] Add a separate CI E2E job with health waiting, failure logs, and unconditional scoped teardown.
- [x] Reconcile the required operational events to consistent `key=value` log fields.
- [x] Reconcile README, DESIGN, and locked decisions with the final authorized scope.
- [x] Run and record the complete final verification matrix.
- [ ] Record the human author's honest focused-hour total in README.
