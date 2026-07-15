# Stage 1 Lifecycle and Stage 2 Security Design

## Boundaries and data

There are exactly two runtime services. Station Service is the source of truth for stations, connectors, connector status, and tariffs. Session Service owns charging sessions, users, and the wallet module; wallet is deliberately not a third service because settlement belongs to the stop transaction. Both services use one PostgreSQL instance for easy local operation, but Station Service owns only the `station` schema and Session Service owns only the `session` schema. They never read one another's tables.

Session Service reaches Station Service through synchronous REST (`RestClient`). This is the required network boundary and keeps the start/stop flow clear: read the connector, occupy or release it remotely, then persist the local session lifecycle. Connection and response waits are explicitly bounded by configurable timeouts (2 and 3 seconds by default). A connection failure, timeout, or unexpected Station response becomes the consistent `503 STATION_SERVICE_UNAVAILABLE` error. There are no hidden retries, fallback paths, brokers, or sagas in this stage.

## Billing and settlement

At start, Session Service verifies the user, requires the connector to be `AVAILABLE`, occupies it, and writes an `ACTIVE` session. It stores a tariff snapshot (price per kWh, start fee, and currency) with that session. This ensures a later station tariff edit cannot rewrite a bill already agreed at session start.

Energy, tariff values, costs, and wallet balances use `BigDecimal`. Stop input is limited to the database's six fractional digits so the value billed is also the value persisted. The final calculation is `energyKwh × pricePerKwh + startFee`, rounded only at the final result to two decimal places using `HALF_UP`. Timestamps use `Instant`; duration is retained for the record but is not part of the price. The negative-balance policy is intentional: the wallet can fall below zero because the energy has already been delivered and must still be settled.

At stop, a pessimistic session row lock serializes settlement. Only an `ACTIVE` session may be completed, so a second stop sees `SESSION_NOT_ACTIVE` before any second debit. Session, wallet, and cost changes share one local database transaction; the connector release runs before that transaction commits. If release cannot connect or exceeds its response timeout, the exception causes the local debit and completion to roll back, leaving the session `ACTIVE` for a later explicit retry.

## Failure trade-offs

The state guard provides practical idempotent-retry reasoning for this limited API: a client retry after a completed stop cannot charge twice because the first committed stop changes the state away from `ACTIVE`. It is not a real idempotency-key protocol, and a retry after an ambiguous network outcome can still require the client to read the session before deciding what happened. Automatic retries are omitted because replaying occupy/release without an idempotency protocol could amplify that ambiguity.

There remains a distributed partial-failure window. A successful remote occupy followed by a local insert failure can leave a connector stuck `OCCUPIED`; a successful remote release followed by a local commit failure can leave an `ACTIVE` session while the connector is `AVAILABLE`. A client-side response timeout bounds the local request but cannot prove whether a remote operation committed. Operational reconciliation could later detect stale sessions/connectors and repair them, or durable messages plus compensating actions could reduce the risk. Those approaches add failure modes and are deliberately not included here.

## Authentication and token design

Authentication is a small module inside Session Service, not a new service. Flyway creates exactly two human auth users and stores only BCrypt hashes. `POST /auth/login` verifies the hash and returns a short-lived HS256 JWT containing the subject, one human role, issued-at/expiry, issuer, and audience. Both backend services validate signature, issuer, audience, and time claims using Spring Security's standard resource-server/JOSE support. Expected filter failures use the same JSON API-error shape as the domain rather than an HTML login page.

`VIEWER` and `ADMIN` live in human tokens because local stateless authorization is the smallest clear design for two services. The trade-off is that a role change or disable action cannot invalidate an already-issued token immediately; the short configurable TTL bounds that delay. HS256 similarly keeps the take-home small but makes the signing secret shared verification material. As the system grew, asymmetric signing would let one issuer hold the private key while resource services receive only public verification keys.

The Station read performed during an ADMIN start delegates the already validated human bearer token, so the public connector read still enforces `VIEWER`/`ADMIN`. Occupy/release never reuse that browser credential. Session Service mints a fresh, short-lived `SERVICE` JWT with a narrow role for those two internal transitions. The browser neither receives nor controls this credential, and no token or Authorization header is logged.

## Browser and network security

The panel stores its access token, username, role, and expiry in `sessionStorage`. This is suitable for a same-tab demo and clears on explicit logout, expiry, or a backend 401, but JavaScript can read it and an XSS issue could steal it. A production alternative would be an `HttpOnly`, `Secure`, `SameSite` cookie issued by a BFF; that reduces JavaScript token exposure while requiring an explicit CSRF strategy and additional deployment complexity.

Frontend role checks only tailor the interface. The backend independently enforces every read and write. Direct calls from a VIEWER therefore receive 403 even if the caller modifies the browser UI. CORS allows one environment-configured origin, the required headers/methods, no wildcard, and no credentials. Production Nginx uses same-origin `/api/session` and `/api/station` proxies and SPA fallback routing.

Security-sensitive logs record successful and failed login plus the actor subject/role and relevant session, user, and connector identifiers on start/stop. They intentionally omit passwords, JWTs, signing secrets, SERVICE credentials, and Authorization headers. These logs provide a small operational audit trail without pretending to be a durable compliance event store.

## Not attempted

Refresh tokens, signup, password reset, OAuth login, token revocation, account lockout, a separate identity provider, cookie/BFF auth, real idempotency keys, automatic retries, circuit breakers, brokers, sagas, distributed transactions, stuck-connector cleanup, caching, service mesh, and the other stretch goals were not implemented.
