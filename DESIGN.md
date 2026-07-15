# ChargeSquare Design Note

## Boundaries and lifecycle

ChargeSquare has exactly two backend services. Station Service owns stations, connectors, status, and tariffs. Session Service owns users, charging sessions, the wallet module, and the small authentication module. They share one PostgreSQL instance for local simplicity but own separate `station` and `session` schemas and never read one another's tables.

Session Service calls Station Service over real synchronous REST. Start verifies the user and an `AVAILABLE` connector, occupies it, snapshots its tariff, and persists an `ACTIVE` session. Stop locks the session row, requires `ACTIVE`, calculates and persists the bill, debits the wallet, completes the session, and releases the connector. Connection and read timeouts are environment-backed; an unavailable or timed-out Station dependency becomes `503 STATION_SERVICE_UNAVAILABLE` with no hidden retry.

Energy, tariff values, cost, and balances use `BigDecimal`. The final rule is `energyKwh x pricePerKwh + startFee`, rounded once with `HALF_UP` to two decimals. A 12.5 kWh stop at 8.50 TRY/kWh plus 2.00 TRY costs 108.25 TRY. The tariff is snapshotted at start so a later tariff edit cannot rewrite an agreed bill. Timestamps use `Instant`, and duration does not affect this tariff. Wallet balances may become negative because delivered energy must still be settled.

## Consistency trade-offs

The pessimistic lock and `ACTIVE` guard prevent a committed stop from charging twice: a repeat receives `409 SESSION_NOT_ACTIVE`. This is useful retry behavior, but it is not an idempotency-key protocol; after an ambiguous client timeout, the caller should read the session before deciding whether to retry.

The network and local database cannot form one atomic transaction. A remote occupy followed by a failed local insert can leave a connector `OCCUPIED`; a remote release followed by a failed local commit can leave an `ACTIVE` session beside an `AVAILABLE` connector. A release timeout rolls back local session/wallet changes, but it cannot prove the remote outcome. Reconciliation, durable messages, or compensating actions could address this in a larger system; they are deliberately documented rather than implemented here.

## Security and reviewability

Session Service verifies BCrypt demo-user hashes and issues short-lived HS256 JWTs with subject, role, issuer, audience, issued-at, and expiry. Both services validate those claims. Human reads require `VIEWER` or `ADMIN`; session writes require `ADMIN`. Session Service creates an independent short-lived `SERVICE` token for Station occupy/release. Browser role checks are UX only; backend enforcement is authoritative. Full token storage, CORS, secret, audit, and limitation details are in [`SECURITY.md`](SECURITY.md).

Both services expose Springdoc OpenAPI JSON and Swagger UI in the local/demo configuration. The contracts document the Bearer scheme, role requirements, stable error body, important statuses, internal transitions, and worked start/stop billing examples. Focused logs use `key=value` events and relevant identifiers without credentials or tokens. `scripts/e2e-smoke.sh` exercises the real authenticated Compose path and is run by a separate CI job.

## Deliberately not attempted

No Wallet Service, reservation, richer tariff, broker/event pipeline, idempotency-key infrastructure, retry or circuit-breaker framework, reconciliation scheduler, API gateway, refresh token, rate limiter, metrics/logging platform, cache, service mesh, saga, or distributed transaction was added. Kubernetes is represented by offline-validatable manifests, not a claimed live-cluster deployment.
