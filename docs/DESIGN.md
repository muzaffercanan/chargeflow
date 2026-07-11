# Stage 1 Session Flow Design

Session Service owns users, wallets, and charging sessions in the PostgreSQL `session` schema. Station Service remains the source of truth for connectors and tariffs; Session Service communicates with it only through synchronous HTTP calls made by Spring `RestClient`.

At start, Session Service checks the local user, reads the connector from Station Service, rejects non-available connectors, occupies it remotely, and then stores an `ACTIVE` session with an immutable tariff snapshot. At stop, a pessimistic database row lock serializes attempts for the same session. The service rejects a session that is no longer `ACTIVE`, calculates the final cost using decimal arithmetic, debits the local wallet, completes the session, and releases the connector. Wallet and session changes share one local transaction, and negative wallet balances are intentionally allowed.

The release HTTP call runs before the local stop transaction commits. A connection failure raises `503 STATION_SERVICE_UNAVAILABLE`, and the unchecked exception rolls back the wallet debit and session completion. This ordering keeps the normal path consistent and handles the most visible dependency-down case without retries or a saga.

There is still an unavoidable distributed partial-failure window: after a successful remote occupy, the local start insert can fail and leave the connector occupied; after a successful remote release, the local stop commit can fail and leave the session active while the connector is available. A production design could reconcile these states or use durable messages and compensating actions. Those mechanisms are explicitly outside Stage 1 scope.
