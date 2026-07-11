# ChargeSquare Implementation Tasks

Status is intentionally conservative: an item is checked only when the corresponding artifact or behavior exists in the repository.

## Foundation

- [x] Create the Maven parent project.
- [x] Create the `station-service` module.
- [x] Create the `session-service` module.
- [x] Add empty Spring Boot application entry points.
- [x] Add the foundation dependencies allowed by the case.
- [x] Add environment-driven service ports and actuator health configuration.
- [x] Verify both empty services start and report healthy through Actuator.
- [ ] Add PostgreSQL connection configuration for both owned schemas.
- [ ] Add Flyway migration locations and schema history configuration.
- [ ] Add deterministic station, connector, tariff, user, and wallet seed migrations.

## Station Service

- [ ] Model stations, connectors, and tariffs in the `station` schema.
- [ ] Implement connector and station connector-list reads.
- [ ] Implement guarded internal occupy/release operations.
- [ ] Add Station Service validation and error mapping.
- [ ] Add Station Service action logging.

## Session Service

- [ ] Model sessions and the wallet module in the `session` schema.
- [ ] Implement the synchronous Station Service client.
- [ ] Implement start validation, tariff snapshot, occupy call, and `ACTIVE` session creation.
- [ ] Implement stop validation, cost calculation, wallet debit, completion, and release call.
- [ ] Implement session and user-session reads.
- [ ] Add consistent API errors, including fail-fast `503` dependency failures.
- [ ] Add lifecycle and billing action logging.

## Tests

- [ ] Add the worked `12.5 * 8.50 + 2.00 = 108.25` cost test.
- [ ] Add a start-to-stop lifecycle test.
- [ ] Add an invalid-case test.
- [ ] Add persistence/integration coverage for the real database-backed flow.

## Docker

- [ ] Add `station-service/Dockerfile`.
- [ ] Add `session-service/Dockerfile`.
- [ ] Add root `docker-compose.yml` with PostgreSQL and both services.
- [ ] Verify the documented one-command Compose flow from a clean checkout.

## Kubernetes/CI

- [ ] Add Station Service Deployment and Service manifests.
- [ ] Add Session Service Deployment and Service manifests.
- [ ] Add a ConfigMap consumed by a Deployment.
- [ ] Validate manifests with `kubectl apply --dry-run=client` or document the unavailable tool.
- [ ] Add push-triggered CI to build, test, and build both images.

## Documentation

- [x] Convert the mandatory Stage 1 requirements into `CASE_REQUIREMENTS.md` checkboxes.
- [x] Record locked choices and explicit non-goals in `DECISIONS.md`.
- [x] Create this implementation task list.
- [x] Add repository guidance in `AGENTS.md`.
- [x] Add short `CLAUDE.md` and `GEMINI.md` agent entry points.
- [ ] Add the top-level README with run instructions, examples, assumptions, tests, and next steps.
- [ ] Add the short design note covering the two required reasoning paragraphs.

## Final Audit

- [ ] Confirm every checked requirement in `CASE_REQUIREMENTS.md` is implemented and tested.
- [ ] Confirm no Stage 2 or stretch-goal artifacts were added.
- [ ] Confirm no secrets or live `.env` files are committed.
- [ ] Confirm no cross-service table access exists.
- [x] Run the root Maven test command.
- [ ] Run the documented Compose flow.
- [ ] Validate Kubernetes manifests.
- [ ] Review README examples against the actual API responses.
