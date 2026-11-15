# Sovmestim — backend

Server-side application for the Sovmestim medication assistant, built as independent Spring Boot
services that talk to each other over **gRPC**. The public REST contract (`v1`) is unchanged and is
served only by the gateway; the mobile client generates its KMP client from the same
`openapi.yaml`.

- Java **25**, Spring Boot **4.0.5**, PostgreSQL 16 (Flyway per service), Testcontainers.
- gRPC **1.84.1** / Protocol Buffers **4.36.2** (`grpc-java`, explicit deadlines, no REST between
  services).
- One Maven reactor. **Every dependency and plugin version is declared in `<properties>` in the
  root `pom.xml`** — child poms contain no versions.

## Architecture

```
mobile client ──HTTP/JSON /v1──> gateway :8080   (JWT edge, OpenAPI, sync orchestration)
                                    │ gRPC
            ┌───────────────┬───────┼──────────────┬──────────────┐
            ▼               ▼       ▼              ▼              ▼
        identity        patient   catalog        intake         advice
        :9091           :9092     :9093          :9094          :9095
            │               │       │              │              │
     sovmestim_       sovmestim_  sovmestim_  sovmestim_     sovmestim_
      identity         patient    catalog      intake         advice
            │               │       │              │              │
        db-identity    db-patient db-catalog   db-intake      db-advice     (one container each)
```

| Module | Responsibility | HTTP | gRPC |
| --- | --- | --- | --- |
| `services/gateway` | REST `/v1`, JWT validation, OpenAPI, sync push/pull | 8080 | client only |
| `services/identity` | users, one-time codes, access/refresh tokens | 8081 | 9091 |
| `services/patient` | allergies, chronic conditions, profile deltas | 8082 | 9092 |
| `services/catalog` | ATC, substances, medicines, search/resolve | 8083 | 9093 |
| `services/intake` | medication courses, intake rules | 8084 | 9094 |
| `services/advice` | interaction sources, engine, advice audit | 8085 | 9095 |
| `proto` | protobuf contracts shared by every service | — | — |
| `common` | error model, entity base, name normalization, gRPC plumbing | — | — |

Rules enforced by the split:

- A service reads **only its own database**. Cross-service ids are plain UUID columns validated
  over gRPC; there are no cross-database foreign keys.
- Only the gateway parses JWTs. Leaf services trust the gateway and receive the user id in the
  request message.
- Every outbound call has a deadline; every request carries `x-correlation-id` through gRPC
  metadata and the log MDC.
- `sync` is orchestration, so it lives in the gateway: it fans out to patient, intake and advice.

## Build and test

Requirements: JDK 25 and Docker (Testcontainers starts PostgreSQL for integration tests).

```bash
export JAVA_HOME=/path/to/jdk-25
./mvnw clean verify
```

- Unit tests (`*Test`, Surefire): advice engine, rule loading, demo source, normalization, JWT,
  gateway error mapping, OpenAPI contract parsing.
- Integration tests (`*IT`, Failsafe) run each service against a **real PostgreSQL container**:
  Flyway migrations, demo catalog seed, sync upsert/conflict/tombstone, login and token rotation,
  gateway OpenAPI mapping. Database behaviour is never mocked.

Run one module:

```bash
./mvnw -pl services/catalog -am verify
./mvnw -pl services/advice -Dtest=AdviceEngineTest test
```

## Run the whole stack

```bash
docker compose up -d --build
# API:        http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health:     http://localhost:8080/actuator/health
# Contract:   http://localhost:8080/openapi.yaml

docker compose logs -f gateway
docker compose down        # stop, keep the databases
docker compose down -v     # stop and drop all databases
```

Smoke test:

```bash
curl -s localhost:8080/actuator/health
curl -s -X POST localhost:8080/v1/auth/request-code \
  -H 'Content-Type: application/json' -d '{"email":"demo@example.com"}'
# demo mode returns devCode; then POST /v1/auth/verify with that code
```

The compose file starts **six PostgreSQL containers**, one per service, plus the six services.
Only the gateway publishes a port; everything else is reachable only inside the compose network.
Copy `.env.example` to `.env` to override `POSTGRES_PASSWORD` and `SOVMESTIM_JWT_SECRET`.

## Run on Kubernetes

Kustomize manifests live in `deploy/k8s`: six Deployments, six PostgreSQL StatefulSets, startup /
readiness / liveness probes, resource requests and limits, non-root containers with a read-only
root filesystem, NetworkPolicies and an optional Ingress.

```bash
./deploy/k8s/build-images.sh 0.2.0
kind create cluster --name sovmestim
for s in identity patient catalog intake advice gateway; do
  kind load docker-image "sovmestim-${s}:0.2.0" --name sovmestim
done

kubectl apply -k deploy/k8s
kubectl -n sovmestim get pods -w
kubectl -n sovmestim port-forward svc/gateway 8080:8080
```

See `deploy/k8s/README.md` for secrets, storage, scaling, rollback and manifest validation.

## gRPC contracts

Contracts live in `proto/src/main/proto/sovmestim/<service>/v1/*.proto` and are compiled by
`protobuf-maven-plugin` into every consumer. Conventions:

- package `sovmestim.<service>.v1`, `java_package ru.sovmestim.proto.<service>.v1`;
- UUIDs are strings, instants are `google.protobuf.Timestamp`, dates are ISO-8601 strings;
- errors use `Status` codes only (`INVALID_ARGUMENT`, `NOT_FOUND`, `ABORTED`, `UNAVAILABLE`);
  `GrpcExceptionInterceptor` maps the shared domain exceptions to them;
- breaking changes require a new package version (`v2`), never field renumbering.

`common` starts the server for every `BindableService` bean when `sovmestim.grpc.port` is set, and
provides the channel factory used by the clients.

## Data

Each service owns one database and its own Flyway history:

| Service | Database | Tables |
| --- | --- | --- |
| identity | `sovmestim_identity` | `app_user`, `gender`, `otp_code`, `refresh_token` |
| patient | `sovmestim_patient` | `allergy*`, `chronic_disease*`, `user_disease`, `mkb`, `severity_reaction`, `status` |
| catalog | `sovmestim_catalog` | `atc`, `active_substance`, `trade_mark`, `form_release`, `unit_measurement`, `medicine`, `substance_in_medicine` |
| intake | `sovmestim_intake` | `intake*`, `taken_substance`, `course_medicine`, `notification*`, `alarm`, `status` |
| advice | `sovmestim_advice` | `advice_record`, `interaction_substances`, `danger_level` |
| gateway | `sovmestim_gateway` | `sync_request` |

Lookup rows (`status`, `intake_type`, `severity_reaction`) are duplicated in the service that reads
them instead of being shared. `course_medicine` stores a medicine name/brand snapshot so reads never
depend on a live catalog call. Legacy tables that no code used (appointments, doctors, weight
tracking, ...) were intentionally not migrated.

## Sync

`POST /v1/sync/push` is idempotent by `idempotencyKey` (the gateway stores the response in
`sync_request`). Each change is applied by its owning service with last-write-wins on server time;
an older client change comes back as `CONFLICT`. After applying a batch the gateway rechecks advice
for the affected courses — or for all active courses when the profile changed.

`GET /v1/sync/pull?cursor=` merges the patient, intake and advice deltas and returns tombstones plus
the newest cursor.

## Adding a dependency or a service

1. Add/change the version **only** in the root `pom.xml` `<properties>` block (and in
   `<dependencyManagement>` if it is a new library).
2. Child modules declare the dependency without a `<version>`.
3. For a new service: add the module to the reactor, a pom with the shared parent, a
   `<service>/v1` proto, a Flyway `V1__..._schema.sql`, its own `application.yml`, a compose
   service with a dedicated database, a Kubernetes manifest in `deploy/k8s/apps` plus its database
   in `deploy/k8s/postgres.yaml` and NetworkPolicy rules, and an integration test against
   Testcontainers.

## API contract (OpenAPI)

The contract stays at `services/gateway/src/main/resources/static/openapi.yaml`, is served at
`/openapi.yaml`, and drives Swagger UI. Three tests keep it honest: `OpenApiSpecTest` (the document
parses and documents exactly the expected v1 operations), `OpenApiContractIT` (registered endpoints
match the contract and both are served), and the compose smoke test.

## Known limitations

- `RlsHttpInteractionSource` is still a thin, untested client; the licensed RLS questions (auth,
  limits, `as_ids` length) must be closed with a real key.
- Drug–disease and drug–food sources return a note / insufficient data.
- Intake events, schedules and attachments are follow-ups; intake exposes substance-level rules
  but the REST contract does not create them yet.
- Rules in `services/advice/src/main/resources/rules/*.yaml` are samples and must be signed by the
  expert pharmacologist before release.
