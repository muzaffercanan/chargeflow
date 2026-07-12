# Stage 1 Design

## Boundaries and data

There are exactly two runtime services. Station Service is the source of truth for stations, connectors, connector status, and tariffs. Session Service owns charging sessions, users, and the wallet module; wallet is deliberately not a third service because settlement belongs to the stop transaction. Both services use one PostgreSQL instance for easy local operation, but Station Service owns only the `station` schema and Session Service owns only the `session` schema. They never read one another's tables.

Session Service reaches Station Service through synchronous REST (`RestClient`). This is the required network boundary and keeps the start/stop flow clear: read the connector, occupy or release it remotely, then persist the local session lifecycle. When Station Service cannot be reached, Session Service fails fast with the consistent `503 STATION_SERVICE_UNAVAILABLE` error. There are no hidden retries, fallback paths, brokers, or sagas in this stage.

## Billing and settlement

At start, Session Service verifies the user, requires the connector to be `AVAILABLE`, occupies it, and writes an `ACTIVE` session. It stores a tariff snapshot (price per kWh, start fee, and currency) with that session. This ensures a later station tariff edit cannot rewrite a bill already agreed at session start.

Energy, tariff values, costs, and wallet balances use `BigDecimal`. The final calculation is `energyKwh × pricePerKwh + startFee`, rounded only at the final result to two decimal places using `HALF_UP`. Timestamps use `Instant`; duration is retained for the record but is not part of the price. The negative-balance policy is intentional: the wallet can fall below zero because the energy has already been delivered and must still be settled.

At stop, a pessimistic session row lock serializes settlement. Only an `ACTIVE` session may be completed, so a second stop sees `SESSION_NOT_ACTIVE` before any second debit. Session, wallet, and cost changes share one local database transaction; the connector release runs before that transaction commits. If the release request cannot reach Station Service, the exception causes the local debit and completion to roll back.

## Failure trade-offs

The state guard provides practical idempotent-retry reasoning for this limited API: a client retry after a completed stop cannot charge twice because the first committed stop changes the state away from `ACTIVE`. It is not a real idempotency-key protocol, and a retry after an ambiguous network outcome can still require the client to read the session before deciding what happened. Implementing idempotency keys, retry/backoff, or exactly-once semantics is intentionally out of scope.

There remains a distributed partial-failure window. A successful remote occupy followed by a local insert failure can leave a connector stuck `OCCUPIED`; a successful remote release followed by a local commit failure can leave an `ACTIVE` session while the connector is `AVAILABLE`. The present code surfaces dependency failure rather than masking it. In a later phase, operational reconciliation could detect stale sessions/connectors and repair them, or durable messages plus compensating actions could reduce the risk. Those approaches add failure modes and are deliberately not included here.

## Next steps

Before production use, add reconciliation and observability around partial failures, well-defined idempotency keys, controlled retry policies, authentication/authorization, database deployment and backup operations, ingress/TLS, scaling, and real meter integration. These are future concerns, not implemented Stage 1 scope.
