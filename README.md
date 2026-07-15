<div align="center">

# ⚡ ChargeSquare Stage 1 + Stage 2

[![CI](https://github.com/muzaffercanan/chargeflow/actions/workflows/ci.yml/badge.svg)](https://github.com/muzaffercanan/chargeflow/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-Panel-61DAFB?logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-persistence-4169E1?logo=postgresql&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-manifests-326CE5?logo=kubernetes&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT%20RBAC-000000?logo=jsonwebtokens&logoColor=white)

</div>

ChargeSquare implements the complete charging-session slice and the optional secured operations panel. The repository still has exactly two backend services: Station Service owns stations, connectors, statuses, and tariffs; Session Service owns sessions, the wallet module, and the small authentication module. A React panel is built as static files and served by Nginx; it is not a third backend service.

Stage 1 remains the regression baseline: `START -> STOP -> BILL -> SETTLE`, PostgreSQL persistence, decimal-safe billing, guarded state transitions, synchronous Session-to-Station REST, Docker, Kubernetes manifests, CI, and focused tests. Stage 2 adds login, JWT validation in both services, backend-enforced RBAC, independent service credentials, and four panel screens. Final optional work adds API documentation, focused structured logs, and a real authenticated Compose smoke test without adding domain features or runtime services.

---

## 🧰 Prerequisites

- Docker Desktop with Docker Engine and Docker Compose v2. The documented flow was tested with Docker Engine `27.3.1` and Compose `2.30.3`.
- For backend builds outside Docker: JDK 21. Maven is supplied by `mvnw`/`mvnw.cmd`.
- For panel development outside Docker: Node.js 20 and npm.
- `curl` for the walkthrough.
- `kubectl` only for optional client-side Kubernetes validation.

---

## 🚀 One-command startup

From a clean checkout:

```sh
docker compose --env-file .env.example up --build --detach
```

The command starts PostgreSQL, Station Service, Session Service, and the panel. Open:

- Panel: `http://localhost:5173`
- Station Service: `http://localhost:8081`
- Session Service: `http://localhost:8082`
- Service health: `GET /health`
- Station Swagger UI: `http://localhost:8081/swagger-ui.html`
- Session Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs` and `http://localhost:8082/v3/api-docs`

The named `postgres-data` volume preserves state across ordinary container restarts.

> [!CAUTION]
> `.env.example` contains deliberately local placeholders only. For anything beyond this local case study, copy it to ignored `.env`, replace both the database password and JWT signing secret, and use `--env-file .env`.

---

## 🔑 Demo accounts

These are public local case-study credentials, not production secrets:

| Role | Username | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| VIEWER | `viewer` | `viewer-demo` | Read connectors, sessions, and receipts. |
| ADMIN | `admin` | `admin-demo` | All VIEWER reads plus start/stop session writes. |

> [!NOTE]
> Only BCrypt hashes are stored in the Flyway migration. Plaintext demo passwords appear here solely so a reviewer can use the local system. The JWT signing secret is never stored in Java or SQL.

---

## 🌱 Deterministic domain seed

| Item | Seed value |
| :--- | :--- |
| Station | `1` - `ChargeSquare Demo Station` |
| Connector `10` | `CCS2-DC`, 60 kW, `AVAILABLE`, tariff `8.50` TRY/kWh plus `2.00` TRY start fee |
| Connector `11` | `Type2-AC`, 22 kW, `AVAILABLE`, same tariff |
| Domain user | `7`, wallet balance `500.00` TRY |

---

## 🛡️ Authorization matrix

| Service | Method and path | Anonymous | VIEWER | ADMIN | SERVICE |
| :--- | :--- | :---: | :---: | :---: | :---: |
| Session | `POST /auth/login` | Allowed | Allowed | Allowed | N/A |
| Both | `GET /health` and probe paths | Allowed | Allowed | Allowed | Allowed |
| Station | `GET /connectors/{id}` | 401 | Allowed | Allowed | 403 |
| Station | `GET /stations/{id}/connectors` | 401 | Allowed | Allowed | 403 |
| Session | `GET /sessions/{id}` | 401 | Allowed | Allowed | 403 |
| Session | `GET /users/{userId}/sessions` | 401 | Allowed | Allowed | 403 |
| Session | `POST /sessions` | 401 | 403 | Allowed | 403 |
| Session | `POST /sessions/{id}/stop` | 401 | 403 | Allowed | 403 |
| Station | `POST /connectors/{id}/occupy` | 401 | 403 | Allowed | Allowed |
| Station | `POST /connectors/{id}/release` | 401 | 403 | Allowed | Allowed |

The `SERVICE` role is internal. Session Service generates a short-lived service JWT for occupy/release and never forwards it to the panel. During an ADMIN start request, the already validated human token is delegated only to Station Service's protected connector read; the subsequent transition uses the independent SERVICE token.

Swagger UI and `/v3/api-docs` are intentionally public in this local/demo configuration so a reviewer can inspect the contracts without first obtaining a token. The OpenAPI documents define the Bearer-JWT scheme, identify public versus `VIEWER`, `ADMIN`, and `SERVICE` operations, describe the stable error body and important statuses, and include start/stop plus `108.25` TRY billing examples. API authorization is unchanged: using Swagger's **Authorize** control supplies a token, but the backend still enforces every protected operation.

Security failures use the same JSON shape as domain failures:

```json
{ "error": "AUTHENTICATION_REQUIRED", "message": "Authentication is required" }
```

```json
{ "error": "ACCESS_DENIED", "message": "You do not have permission to perform this operation" }
```

---

## 📡 Authenticated API walkthrough

Log in and copy `accessToken` from the response:

```sh
curl --fail-with-body --request POST http://localhost:8082/auth/login \
  --header 'Content-Type: application/json' \
  --data '{"username":"admin","password":"admin-demo"}'
```

Set the copied value for the commands below:

```sh
export ADMIN_TOKEN='<accessToken>'
```

Inspect the station:

```sh
curl --fail-with-body http://localhost:8081/stations/1/connectors \
  --header "Authorization: Bearer $ADMIN_TOKEN"
```

Start a session. The Session Service validates connector `10`, delegates the protected read, uses its own SERVICE JWT to occupy it, snapshots the tariff, and returns `201`:

```sh
curl --fail-with-body --request POST http://localhost:8082/sessions \
  --header "Authorization: Bearer $ADMIN_TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{"userId":7,"connectorId":10}'
```

Use the returned `sessionId` to stop it. For the first clean-volume session it is normally `1`:

```sh
curl --fail-with-body --request POST http://localhost:8082/sessions/1/stop \
  --header "Authorization: Bearer $ADMIN_TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{"energyKwh":12.5}'
```

The calculation is `12.5 x 8.50 + 2.00 = 108.25` TRY and the seeded wallet becomes `391.75` TRY. Review it with:

```sh
curl --fail-with-body http://localhost:8082/sessions/1 \
  --header "Authorization: Bearer $ADMIN_TOKEN"
curl --fail-with-body http://localhost:8082/users/7/sessions \
  --header "Authorization: Bearer $ADMIN_TOKEN"
```

To verify the role boundary, log in as VIEWER and send its token to `POST /sessions`; the backend returns `403 ACCESS_DENIED` even if a caller bypasses the panel.

---

## 🖥️ Panel behavior

The panel contains approximately four screens as requested:

1. Login with labeled fields, keyboard submission, loading, and invalid-credential feedback.
2. Station/connectors table with status, type, power, price, start fee, refresh, loading, empty, and error states.
3. Seeded-user session history with receipt links and billing fields.
4. Session receipt/detail with tariff snapshot and an ADMIN-only active-session stop form.

The panel stores the access token, username, role, and calculated expiry in `sessionStorage`, never the password. Same-tab refresh survives; explicit logout, token expiry, or any API 401 clears the state and returns to login. A 403 remains visible as access denied.

> [!WARNING]
> `sessionStorage` is accessible to JavaScript and therefore exposed if an XSS bug exists. A production system would normally consider a BFF or an `HttpOnly`, `Secure`, `SameSite` cookie. That reduces direct token exposure but introduces cookie/CSRF design work, so it is deliberately not simulated in this take-home. UI role checks are convenience only; the backend always makes the authorization decision.

---

## 🧪 Local development and tests

Backend tests and verification:

```sh
./mvnw test
./mvnw verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Panel development and tests:

```sh
cd panel
npm ci
npm test
npm run build
npm run dev
```

Build the three images and validate Compose configuration:

```sh
docker build --tag chargesquare/station-service:local --file station-service/Dockerfile .
docker build --tag chargesquare/session-service:local --file session-service/Dockerfile .
docker build --tag chargesquare/panel:local --file panel/Dockerfile .
docker compose --env-file .env.example config --quiet
```

Run the real authenticated cross-service smoke test against a clean, healthy Compose stack:

```sh
docker compose --env-file .env.example down --volumes --remove-orphans
docker compose --env-file .env.example up --build --detach --wait
bash scripts/e2e-smoke.sh
docker compose --env-file .env.example down --volumes --remove-orphans
```

The script requires `curl` and `jq`, discovers an available connector, captures the generated session id, verifies VIEWER read/403 behavior, runs ADMIN start/stop through both services, checks the `108.25` receipt and `391.75` wallet balance, checks connector transitions, confirms second-stop `409`, and reads the persisted receipt. It exits non-zero on any mismatch and assumes the deterministic clean-volume wallet seed.

Vite serves `http://localhost:5173` and proxies `/api/session` to port `8082` and `/api/station` to port `8081`, so backend CORS remains narrow and API base URLs are not duplicated through components. `VITE_DEMO_USER_ID` and `VITE_DEMO_STATION_ID` may override the UI defaults `7` and `1` at build/development time.

The frontend test suite covers successful/failed login, anonymous redirect, VIEWER/ADMIN action UX, 401 state clearing, 403 messaging, stop-and-refresh settlement, and loading state. The backend suite explicitly exercises login, disabled users, invalid/expired tokens, role authorization, SERVICE transitions, billing/lifecycle, timeout rollback, tariff snapshotting, and double-stop protection. Security is not globally disabled in tests.

CI keeps the original Maven/frontend/image job and adds a separate authenticated E2E job. That job verifies the backend, builds and starts a project-scoped Compose stack, waits for health, runs `scripts/e2e-smoke.sh`, prints service logs on failure, and always removes its containers and volumes. It neither pushes images nor deploys.

### ✅ Final verification snapshot

> [!TIP]
> Executed on 2026-07-15: Maven `test` and `verify` passed all 44 backend tests (13 Station and 31 Session); `npm ci`, 9 frontend tests, the production build, and npm audit passed; all three Dockerfiles built; Compose configuration, clean startup, health waiting, the authenticated E2E smoke test, both live Swagger/OpenAPI endpoints, the live VIEWER/ADMIN/SERVICE matrix, timeout/rollback regression, and restart persistence passed. Shellcheck, actionlint, `git diff --check`, and the tracked/working-tree secret scans also passed after excluding generated output and the documented deterministic test-only JWT key. Kubernetes results are stated separately below because no cluster API was available.

---

## ⚙️ Environment configuration

| Variable | Purpose | Local example/default |
| :--- | :--- | :--- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Local PostgreSQL container | placeholders in `.env.example` |
| `STATION_DB_URL`, `SESSION_DB_URL` | Service JDBC URLs | wired by Compose/Kubernetes |
| `STATION_DB_USERNAME`, `SESSION_DB_USERNAME` | Database user | secret/environment |
| `STATION_DB_PASSWORD`, `SESSION_DB_PASSWORD` | Database password | secret/environment |
| `STATION_DB_SCHEMA`, `SESSION_DB_SCHEMA` | Owned schemas | `station`, `session` |
| `STATION_SERVICE_PORT`, `SESSION_SERVICE_PORT` | Backend HTTP ports | `8081`, `8082` |
| `STATION_SERVICE_URL` | Session-to-Station base URL | `http://station-service:8081` |
| `STATION_CONNECT_TIMEOUT`, `STATION_READ_TIMEOUT` | Bounded downstream waits | `2s`, `3s` |
| `JWT_SIGNING_SECRET` | Shared HS256 verification/signing key, minimum 32 UTF-8 bytes | local placeholder only |
| `JWT_ACCESS_TTL` | Human access-token lifetime | `15m` |
| `JWT_SERVICE_TTL` | Internal SERVICE-token lifetime | `60s` |
| `JWT_ISSUER`, `JWT_AUDIENCE` | Required token issuer/audience | `chargesquare-session-service`, `chargesquare-api` |
| `PANEL_ALLOWED_ORIGIN` | Exact Vite browser origin allowed by CORS | `http://localhost:5173` |
| `PANEL_PORT` | Host port for Nginx panel | `5173` |

> [!NOTE]
> No wildcard CORS origin or credentialed browser request is enabled. Only `Authorization`, `Content-Type`, and the required GET/POST/OPTIONS methods are allowed.

---

## ☸️ Kubernetes

The manifests in [`k8s`](k8s) expect externally reachable PostgreSQL and two pre-created secrets. No Secret manifest or value is committed:

```sh
kubectl create secret generic chargesquare-postgres \
  --from-literal=username='<postgres-user>' \
  --from-literal=password='<postgres-password>'
kubectl create secret generic chargesquare-jwt \
  --from-literal=signing-secret='<at-least-32-byte-random-secret>'
kubectl apply --dry-run=client -f k8s/
```

`chargesquare-config` supplies service URLs, ports, schemas, JDBC URLs, token TTLs, issuer/audience, and CORS origin. Both Deployments obtain `JWT_SIGNING_SECRET` via `secretKeyRef`. In a real environment the secret would be generated and rotated through the platform's secret manager, not a shell history or committed YAML.

> [!NOTE]
> Final validation on 2026-07-15 did not claim a cluster deployment: `kubectl apply --dry-run=client -f k8s/` could not download the schema because no Kubernetes API server was configured at `localhost:8080`. Offline `kubeconform v0.6.7 -strict -kubernetes-version 1.30.0` validation found all five resources valid (`5 valid, 0 invalid, 0 errors, 0 skipped`).

---

## 🏗️ Architecture and security notes

Java 21 and Spring Boot 3.5.16 provide web, validation, JPA, Flyway, Actuator, Spring Security resource-server, and JOSE support. PostgreSQL holds durable state in service-owned schemas. `BigDecimal` is used for energy, tariffs, cost, and wallet balance; only the final bill is rounded `HALF_UP` to two decimals. `Instant` is used for persisted/API timestamps. Tariffs are snapshotted at session start.

Human JWT roles are claims so both services can make stateless local decisions. That is small and fast but means a role change takes effect when the short access token expires; a larger system could use asymmetric signing and centralized policy/user lookup where immediate revocation is required. HS256 is acceptable for these two tightly controlled services but requires careful shared-secret distribution; asymmetric keys would reduce the number of signers as the service count grows.

Security-relevant logs use consistent `key=value` fields for `login_succeeded`, `login_failed`, `session_started`, `session_stopped`, `wallet_debited`, `connector_occupied`, `connector_released`, useful authorization failures, and Station dependency timeouts. Fields are limited to relevant actor/role/resource and billing identifiers. Passwords, JWTs, signing secrets, Authorization headers, and SERVICE credentials are never logged.

See [`DESIGN.md`](DESIGN.md) for lifecycle and partial-failure trade-offs, and [`SECURITY.md`](SECURITY.md) for the implemented authentication, authorization, browser-storage, CORS, secrets, audit, and limitation details.

---

## 📋 Assumptions, exclusions, and known limitations

- Meter energy is supplied by the stop request; no physical meter integration exists.
- Wallet balances may become negative because delivered energy must still be settled.
- Station dependency failures return `503 STATION_SERVICE_UNAVAILABLE`; start persists no session, and release timeout rolls back local stop/wallet updates. There are no automatic retries.
- Auth users are migration-seeded demo accounts. There is no signup, password reset, OAuth login, refresh token, account lockout, token revocation, or identity provider.
- Token role changes are not immediate; access tokens are intentionally short-lived.
- Panel session state has the documented XSS trade-off and is not shared across browser tabs.
- The panel uses the configured seeded station/user IDs rather than discovery or search APIs.
- Wallet top-up, reservations, time-of-use tariffs, real idempotency keys, stuck-connector cleanup, domain events, broker/saga machinery, a retry or circuit-breaker framework, service mesh, cache, rate limiting, metrics/logging platforms, ingress, HPA, API gateway, and a third Wallet Service remain out of scope.
- The Kubernetes set deploys the two backend services only; the panel is supplied through Docker/Compose for the requested Stage 2 slice.

**Optional features attempted:** complete Stage 2 authentication/RBAC backend and four-screen operations panel; root security design; Springdoc OpenAPI/Swagger for both services; focused structured operational logs; and an authenticated cross-service Compose smoke test with CI execution.

**Optional features deliberately not attempted:** new domain features, a Wallet Service, broker/event pipeline, idempotency-key infrastructure, retry/circuit-breaker machinery, reconciliation jobs, gateway, refresh tokens, rate limiting, or an observability platform.

> [!CAUTION]
> **Time spent:** The human author's focused-hour total was not supplied during the final automated audit. Replace this sentence with the honest approximate total before submission.
