# ChargeSquare Stage 1

ChargeSquare contains exactly two Spring Boot services: Station Service owns stations, connectors, and tariffs; Session Service owns sessions and the wallet module. They use one PostgreSQL instance while retaining schema ownership (`station` and `session` respectively). Session Service calls Station Service synchronously over REST for the start and stop lifecycle.

## Prerequisites

- Docker Desktop with Docker Engine and Docker Compose v2, running locally. The documented startup was tested with Docker Engine `27.3.1`.
- For Maven tests outside Docker: JDK `21` and an internet connection on the first Maven Wrapper run. Maven itself is supplied by `mvnw`/`mvnw.cmd`.
- `curl` for the walkthrough below.
- `kubectl` only for the optional client-side manifest validation.

## Start locally

From a clean checkout, the tested one-command startup is:

```sh
docker compose --env-file .env.example up --build --detach
```

It starts PostgreSQL, Station Service, and Session Service. The named `postgres-data` volume preserves data across normal container restarts. The placeholder password in `.env.example` is deliberately non-secret and is suitable only for local development. To set a local password, copy it to `.env`, change `POSTGRES_PASSWORD`, and use `--env-file .env`.

With the example environment, Station Service is at `http://localhost:8081` and Session Service at `http://localhost:8082`. Both expose `GET /health`.

## Deterministic seed data

| Item | Seed value |
| --- | --- |
| Station | `1` — `ChargeSquare Demo Station` |
| Connector `10` | `CCS2-DC`, 60 kW, `AVAILABLE`, tariff `8.50` TRY/kWh plus `2.00` TRY start fee |
| Connector `11` | `Type2-AC`, 22 kW, `AVAILABLE`, same tariff |
| User | `7`, wallet balance `500.00` TRY |

## Main endpoints

| Service | Method and path | Purpose |
| --- | --- | --- |
| Station | `GET /connectors/{id}` | Read a connector and its tariff. |
| Station | `GET /stations/{id}/connectors` | List a station's connectors. |
| Station | `POST /connectors/{id}/occupy` | Internal guarded transition to `OCCUPIED`. |
| Station | `POST /connectors/{id}/release` | Internal, idempotent release to `AVAILABLE`. |
| Session | `POST /sessions` | Start a session for `userId` and `connectorId`. |
| Session | `POST /sessions/{id}/stop` | Stop and settle a session with `energyKwh`. |
| Session | `GET /sessions/{id}` | Read one session and its tariff snapshot. |
| Session | `GET /users/{userId}/sessions` | List a user's session history. |
| Both | `GET /health` | Actuator health endpoint. |

## Curl walkthroughs

Run these against a new Compose volume. If a previous run already created sessions, substitute the `sessionId` returned by the start response for `1` below.

Inspect the seeded station state:

```sh
curl --fail-with-body http://localhost:8081/connectors/10
curl --fail-with-body http://localhost:8081/stations/1/connectors
```

Start and stop the first session. The stop response has `cost: 108.25` and `walletBalanceAfter: 391.75`.

```sh
curl --fail-with-body --request POST http://localhost:8082/sessions \
  --header 'Content-Type: application/json' \
  --data '{"userId":7,"connectorId":10}'

curl --fail-with-body --request POST http://localhost:8082/sessions/1/stop \
  --header 'Content-Type: application/json' \
  --data '{"energyKwh":12.5}'
```

Review the completed session and user history:

```sh
curl --fail-with-body http://localhost:8082/sessions/1
curl --fail-with-body http://localhost:8082/users/7/sessions
```

The calculation is `12.5 × 8.50 + 2.00 = 108.25` TRY. From the `500.00` TRY seed balance, the resulting wallet balance is `391.75` TRY.

## Test and build

Run the complete automated test suite with the repository Maven Wrapper:

```sh
./mvnw test
```

On Windows PowerShell, use:

```powershell
.\mvnw.cmd test
```

The GitHub Actions workflow runs on every push and pull request. It installs Java 21, builds both Maven modules, runs every test, and builds both Docker images without pushing or deploying them.

## Configuration

All operational configuration is environment-backed. The `.env.example` file has placeholders only and no live secret.

| Variable | Used by | Example/default |
| --- | --- | --- |
| `STATION_DB_URL`, `SESSION_DB_URL` | Database connections | `jdbc:postgresql://postgres:5432/chargesquare` |
| `STATION_DB_USERNAME`, `SESSION_DB_USERNAME` | Database credentials | supplied from the environment/secret |
| `STATION_DB_PASSWORD`, `SESSION_DB_PASSWORD` | Database credentials | supplied from the environment/secret |
| `STATION_DB_SCHEMA`, `SESSION_DB_SCHEMA` | Schema ownership | `station`, `session` |
| `STATION_SERVICE_PORT`, `SESSION_SERVICE_PORT` | HTTP ports | `8081`, `8082` |
| `STATION_SERVICE_URL` | Session-to-Station REST base URL | `http://station-service:8081` |
| `STATION_CONNECT_TIMEOUT` | Session-to-Station connection timeout | `2s` |
| `STATION_READ_TIMEOUT` | Session-to-Station response/read timeout | `3s` |

Kubernetes manifests are in [`k8s`](k8s). They expect a reachable PostgreSQL database at the host in the ConfigMap; this repository intentionally does not include a database Deployment. Before applying them, create a secret named `chargesquare-postgres` with `username` and `password` keys, then replace the non-secret database host/name in `k8s/configmap.yaml` if needed. No database credential values are committed.

```sh
kubectl create secret generic chargesquare-postgres \
  --from-literal=username='<postgres-user>' \
  --from-literal=password='<postgres-password>'
kubectl apply --dry-run=client -f k8s/
```

The manifests use `ConfigMap` values for service URLs, ports, schemas, and JDBC URLs, explicit `secretKeyRef` values for database credentials, and `/health/readiness` plus `/health/liveness` probes. The required dry-run command was attempted with kubectl `1.30.5`, but this workspace has no configured Kubernetes API server; that kubectl version performs OpenAPI discovery and exited while trying `localhost:8080`. As an offline fallback, kubeconform `0.6.7` validated all five resources in strict mode. No cluster deployment is claimed; run the same kubectl command from a configured cluster context to complete API-server validation.

## Architecture and choices

Java 21 and Spring Boot 3.5.16 provide the requested current LTS/runtime stack with web, validation, JPA, Flyway, Actuator, and test support. PostgreSQL provides durable state, while Flyway produces the two owned schemas and deterministic seeds from a clean database. `BigDecimal` is used for energy, tariffs, costs, and wallet balances; final cost uses `HALF_UP` rounding to two decimal places. The tariff is snapshotted when a session starts, so later tariff changes do not alter a completed bill.

For the detailed lifecycle and failure trade-offs, see [`DESIGN.md`](DESIGN.md).

## Assumptions and known gaps

- Meter energy is supplied by the stop request; no real meter integration exists.
- Wallet balances may become negative so delivered energy is always settled.
- If Station Service cannot be connected to or does not respond within the configured timeout, Session Service returns `503 STATION_SERVICE_UNAVAILABLE`; start persists no session, and a timed-out release rolls back the local stop and wallet debit. It does not retry or fall back.
- No authentication, authorization, Stage 2 administration, top-ups, reservations, time-of-use tariffs, real idempotency keys, stuck-connector cleanup, domain events, OpenAPI, or advanced observability is implemented.
- No retries/backoff, broker, saga, exactly-once delivery, cache, rate limiting, service mesh, ingress, HPA, refresh tokens, or Kubernetes database Deployment is implemented.

**Optional features attempted:** none.

**Time spent:** Approximately `[ACTUAL_FOCUSED_HOURS]` focused hours.
