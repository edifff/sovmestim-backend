# Load probe

A small k6 script used to size the deployment. It is a smoke/load probe, not a full benchmark:
the demo dataset is tiny, so the numbers are an **upper bound** for the code paths, not a guarantee
for a production catalog.

## Run

```bash
docker compose up -d
# MODE: catalog | profile | advice | sync
docker run --rm --network host -v "$PWD/deploy/loadtest/k6.js:/script.js" \
  -e MODE=catalog grafana/k6:latest run /script.js
```

For a cluster, port-forward the gateway and set `BASE`:

```bash
kubectl -n sovmestim port-forward svc/gateway 8080:8080
docker run --rm --network host -v "$PWD/deploy/loadtest/k6.js:/script.js" \
  -e MODE=advice -e BASE=http://localhost:8080 grafana/k6:latest run /script.js
```

`setup()` creates one user, seeds one medication and reuses its token. The `sync` mode pushes the
same allergy repeatedly (a deliberately hot row) and triggers the advice recheck loop.

## Measured baseline

Hardware: 16 vCPU AMD Ryzen 7 7840HS, 14 GiB RAM, Docker Compose (all services and databases on the
same host), demo mode, 50 VUs, 30 s.

| Mode | Throughput | p95 latency | Errors |
| --- | --- | --- | --- |
| `catalog` (public substance search) | ~7,300 req/s | 8.3 ms | 0% |
| `profile` (GET allergies) | ~12,300 req/s | 4.5 ms | 0% |
| `advice` (check with demo source) | ~1,430 req/s | 34 ms | 0% |
| `sync` (push + advice recheck) | ~3,000 req/s | 18 ms | 0.01% |

Container memory: ~2.3 GiB idle for the whole stack (apps 320–366 MiB, databases ~43 MiB);
~3.6 GiB after the load runs (JVM heaps grown, high-water mark).

Expect lower numbers with a real catalog (the substance search uses `LIKE '%q%'`), with
interactions/findings in the advice result, and with `SOVMESTIM_RLS_MODE=http`, where the external
RLS API latency dominates.
