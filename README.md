# Sovmestim — backend

Server-side application for the Sovmestim medication assistant (roadmap phases 1–2, server vertical
slices). The decision logic is server-only: the mobile client holds no rules and no full drug
directory (ADR-002).

- Java **25**, Spring Boot **4.0.5**, PostgreSQL (Flyway-managed schema), Testcontainers.
- Built on the provided database script (`deepseek_sql_20261003_cca4c7.sql`), used verbatim as the
  base migration.
- RLS (the licensed Russian drug-registry API) sits behind the `InteractionSource` interface; a
  recorded **demo mode** lets the whole check pipeline run with zero paid calls.

Detailed design: `sovmestim-architecture.en.md`. Plan: `sovmestim-roadmap.en.md`.

## Requirements

- JDK 25 (`java -version` → 25.x). If it is not the default, point `JAVA_HOME` at it, e.g.:

  ```bash
  export JAVA_HOME=/home/edifff/.jdks/jbr-25.0.2
  ```

- Docker (used by the integration tests to start PostgreSQL 16 via Testcontainers).
- Maven wrapper is included; do not use a system Maven.

## Build and test

```bash
export JAVA_HOME=/home/edifff/.jdks/jbr-25.0.2
./mvnw clean verify
```

- Unit tests (`*Test`) run with Surefire: name normalization, class-mapping loader, allergy rules,
  demo interaction source, advice engine.
- Integration tests (`*IT`) run with Failsafe against a real PostgreSQL container: Flyway
  migrations + demo seed, passwordless auth, catalog search/resolve, advice check end-to-end, and
  profile sync push/pull. Per `AGENTS.md`, database behavior is never mocked.

Run a single test:

```bash
./mvnw -Dtest=AdviceEngineTest test
./mvnw -Dit.test=AdviceIT verify
```

## Running the application

The app needs PostgreSQL with the provided schema. Flyway applies the migrations on startup
(`V1__init_schema.sql` from the supplied script, `V2__application_tables.sql` adds login and audit
tables). Configuration is environment-driven (`src/main/resources/application.yml`):

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/sovmestim` | database |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `sovmestim` | database credentials |
| `SOVMESTIM_JWT_SECRET` | dev value | HS256 secret, ≥ 32 bytes |
| `SOVMESTIM_OTP_DEBUG` | `true` | return the login code in the response (dev only) |
| `SOVMESTIM_RLS_MODE` | `demo` | `demo` = recorded responses, `http` = licensed RLS API |
| `SOVMESTIM_RLS_BASE_URL` / `SOVMESTIM_RLS_API_KEY` | — | RLS credentials for `http` mode |
| `SOVMESTIM_DEMO_SEED` | `true` | seed the recorded catalog when the tables are empty |

```bash
./mvnw spring-boot:run
# or
java -jar target/sovmestim-backend-0.1.0-SNAPSHOT.jar
```

## Run locally with Docker Compose

The fastest way to run the whole stack (API + PostgreSQL). Demo mode is on, so the recorded catalog
and interactions are seeded and login codes are returned in the response.

```bash
docker compose up -d --build
# API:        http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health:     http://localhost:8080/actuator/health
# Contract:   http://localhost:8080/openapi.yaml

docker compose logs -f api
docker compose down        # stop, keep the database volume
docker compose down -v     # stop and drop the database volume
```

Smoke test:

```bash
curl -s localhost:8080/actuator/health
# request a code; in debug mode devCode is returned in the body
curl -s -X POST localhost:8080/v1/auth/request-code \
  -H 'Content-Type: application/json' -d '{"email":"demo@example.com"}'
```

Copy `.env.example` to `.env` to override `POSTGRES_PASSWORD` and `SOVMESTIM_JWT_SECRET`.

## API

All endpoints are prefixed with `/v1`. Catalog reads are public; everything else needs
`Authorization: Bearer <accessToken>`.

- `POST /auth/request-code`, `POST /auth/verify`, `POST /auth/refresh` — passwordless login
  (`OtpSender` interface, e-mail/Telegram/SMS later).
- `GET /catalog/substances?query=`, `GET /catalog/medicines?query=`, `GET /catalog/medicines/{id}`,
  `GET /catalog/resolve?name=` — normalization + autocomplete + drug→substance resolution.
- `GET /profile`, `GET|POST|PUT|DELETE /profile/allergies`, `GET|POST|DELETE /profile/conditions`.
- `GET|POST|DELETE /medications` — current drugs (courses) feeding the check.
- `POST /advice/check` — online compatibility check (`drugName`, `medicineId` or `substanceIds`).
- `POST /advice/recheck` — recompute advice for all active medications (rules/catalog update trigger).
- `POST /sync/push`, `GET /sync/pull?cursor=` — profile **and** medication sync; push is idempotent
  and runs advice checks automatically, whose results arrive as `advice` on the next pull.

## Sync and the server-side advice loop

`POST /v1/sync/push` accepts allergies, conditions and current medications, is idempotent by
`idempotencyKey` (a retry returns the stored response), and applies last-write-wins per record on
server time (older client changes come back as `CONFLICT`). After applying a batch the server
auto-runs checks for new/changed medications — or for all active medications when the profile
changed — and stores `advice_record` rows.

`GET /v1/sync/pull?cursor=` returns everything changed since the cursor: profile, medications,
tombstones, and the new advice delivery (danger level, findings, sources, rule/catalog versions),
plus the next cursor. `POST /v1/advice/recheck` recomputes advice for all active medications after a
rules or catalog update.

## API contract (OpenAPI)

The contract is authored first and lives at `src/main/resources/static/openapi.yaml`; it is the
source of truth for the v1 API and is served at `/openapi.yaml`. **Swagger UI** is served at
`/swagger-ui.html` and loads that same file (the generated `/v3/api-docs` is not used by the UI).
Three tests keep the implementation honest:

- `OpenApiSpecTest` — the spec parses without errors and documents exactly the expected v1 operations.
- `OpenApiContractIT` — every endpoint the application registers matches the spec (and vice versa).
- `OpenApiValidationIT` — real requests and responses from the running app are validated against the
  spec with the Atlassian OpenAPI request validator; response-shape or status drift fails the build.

To change the API: edit `openapi.yaml`, update the controller, and run `./mvnw verify`. The generated
KMP client is produced from this file.

```bash
./mvnw -Dtest=OpenApiSpecTest test
./mvnw -Dit.test=OpenApiContractIT,OpenApiValidationIT verify
```

## Advice pipeline

`AdviceEngine` (framework-free) composes, without inventing anything:

1. **Drug–drug**: `InteractionSource` results. Sources are merged: the demo source (recorded
   `interact_v2` responses) or the live HTTP client, plus the own `interaction_substances` cache.
2. **Class mapping**: `rules/rls_class_mapping.yaml` maps RLS `class`/`subclass`/`direction` to a
   danger level (`INFO`/`CAUTION`/`AVOID`/`FORBIDDEN`) and an explanation template.
3. **Drug–allergy**: own rule (`rules/allergy_rules.yaml`), matching by substance name, ATC prefix
   or cross-reactivity group.
4. **Duplicate active substance**.

An empty source response becomes `NO_INTERACTIONS_REPORTED` ("RLS returned no interactions"), an
unresolved name becomes `INSUFFICIENT_DATA`. The status `safe` does not exist. Every check is stored
in `advice_record` with the rules/catalog versions.

## Layout

```
src/main/java/ru/sovmestim
├── config        security, JWT, properties, rule loading
├── common        errors, name normalization, current-user helper
├── identity      users, OTP/refresh login
├── patient       allergies, conditions, snapshot for the engine
├── catalog       ATC/substances/medicines, demo/RLS import
├── advice        engine, sources, class mapping, allergy rules, audit
├── intake        current medications (course medicines)
└── sync          profile push/pull with cursor
```

## Known limitations / next steps

- `RlsHttpInteractionSource` is a thin, untested client: the open questions from architecture §9
  (licensed method, auth, limits, `as_ids` length) must be closed with a real test key.
- Drug–disease and drug–food sources are undefined and return a note / insufficient data.
- Sync covers the profile and current medications, with idempotent push and server-side auto-check;
  intake events, schedules and attachments are follow-ups.
- Rules in `rules/*.yaml` are samples and must be signed by the expert pharmacologist before release.
