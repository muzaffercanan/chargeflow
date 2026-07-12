# ChargeSquare Stage 1

Run the complete local stack from a clean checkout with Docker Desktop running:

```sh
docker compose --env-file .env.example up --build
```

The command starts one PostgreSQL container and the Station and Session services. PostgreSQL data is stored in the named `postgres-data` volume, so normal service restarts retain the seeded data and completed sessions. To use a non-placeholder local password, copy `.env.example` to `.env`, update `POSTGRES_PASSWORD`, and pass `--env-file .env` instead.

Service endpoints are available on `http://localhost:8081` (Station Service) and `http://localhost:8082` (Session Service) with the example environment. Each service exposes `GET /health`.

For the deterministic walkthrough, connector `10` is seeded as `AVAILABLE`; starting a session for user `7`, then stopping it with `12.5` kWh, produces a `108.25` TRY cost and a `391.75` TRY wallet balance.
