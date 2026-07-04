# Deployment

How to build, run, and configure Payment Core. The only supported deployment today is Docker Compose for local and demo use; the `k8s/` directory exists but is empty, and the production checklist below lists what must change before this service faces real traffic.

Related documents: [ARCHITECTURE.md](ARCHITECTURE.md), [SECURITY.md](SECURITY.md), [PERFORMANCE.md](PERFORMANCE.md).

## Build

```bash
# Jar (requires Java 21+ and Maven)
mvn clean package

# Docker image
docker build -t payment-core:latest .
```

The Dockerfile is a two-stage build: a Maven/Corretto builder stage that caches dependencies via `mvn dependency:go-offline` before copying sources (so dependency downloads are only invalidated when `pom.xml` changes), and an `amazoncorretto:23-alpine` runtime stage that carries just the jar.

The runtime entrypoint currently starts the JVM with the JDWP debug agent listening on port 5005. This is convenient for attaching a debugger to the container but must be removed for any non-local deployment; see the production checklist.

## Configuration via .env

Copy `.env.example` to `.env` and adjust values as needed:

```bash
cp .env.example .env
```

`.env` is gitignored. Docker Compose auto-loads a root-level `.env` for `${VAR}` substitution inside `docker-compose.yml` — no extra flag needed — and passes the resolved values into containers as real environment variables, which Spring Boot reads natively (no library involved).

Neither the app nor k6 reads `.env` automatically when run outside Docker (`mvn spring-boot:run`, `java -jar`, or the k6 scripts standalone). Export it into your shell first:

```bash
set -a; source .env; set +a
```

The checked-in `.env.example` defaults match what used to be hardcoded in `docker-compose.yml` and `application.yml` — dev-only placeholders, not real secrets (see [SECURITY.md](SECURITY.md)).

## Running with Docker Compose

```bash
docker-compose up -d          # start everything
docker-compose logs -f payment-core
docker-compose up -d --build payment-core   # rebuild after code changes
docker-compose down           # stop
```

### Service topology

| Service | Image | Ports | Purpose |
|---|---|---|---|
| `postgres` | postgres:15-alpine | 5432 | Application database (`payment_db`) |
| `zookeeper` | cp-zookeeper:7.4.0 | 2181 | Kafka coordination |
| `kafka` | cp-kafka:7.4.0 | 9092 | Event streaming |
| `prometheus` | prom/prometheus | 9090 | Metrics scraping |
| `jaeger` | jaegertracing/all-in-one | 16686 (UI), 9411 (collector) | Distributed tracing, plus Service Performance Monitor (reads RED metrics from Prometheus) |
| `otel-collector` | otel/opentelemetry-collector-contrib | 8889 (span metrics) | Forwards spans to Jaeger unchanged while deriving SPM metrics from them |
| `grafana` | grafana/grafana | 3000 | Dashboards over Prometheus and Jaeger, auto-provisioned from `grafana/` |
| `payment-core` | built from `Dockerfile` | 8080 (HTTP), 5005 (debug) | The application |
| `db-seed` | postgres:15-alpine | - | Seeds the load test user, then exits |

All services share the `payment-network` bridge network. Postgres, Prometheus, and Grafana data live in named volumes (`postgres_data`, `prometheus_data`, `grafana_data`) and survive `docker-compose down`; use `down -v` to wipe them.

### Startup ordering

Compose enforces a healthcheck chain rather than plain start ordering:

1. `postgres`, `zookeeper`, `prometheus`, `jaeger` become healthy.
2. `kafka` starts after `zookeeper` is healthy.
3. `otel-collector` starts after `jaeger` is healthy. It has no container healthcheck of its own — the `otelcol-contrib` image ships only the collector binary, with no shell or `wget`/`curl` to run a check with — so dependents use `service_started` rather than `service_healthy` for it; the collector is ready within about a second of starting.
4. `grafana` starts after `prometheus` and `jaeger` are healthy, and provisions its datasources and dashboards from `grafana/` on that same startup — nothing to import by hand.
5. `payment-core` starts after `postgres`, `kafka`, `prometheus`, `jaeger`, and `otel-collector` are all up, and is itself considered healthy once `/actuator/health` responds.
6. `db-seed` runs `loadtest/seed-load-test-user.sql` only after `payment-core` is healthy.

The seed job waits on the application rather than on Postgres because the app's Flyway run is what creates the `users` and `user_roles` tables — seeding after Postgres alone would race the migrations. It also reruns on every startup, not just the first, because the default `DB_RESET_ON_STARTUP=true` wipes the schema on each boot (see below).

## Configuration

Configuration lives in `src/main/resources/application.yml` and is overridden through environment variables, sourced from `.env` (see above). The compose file derives the datasource from the same variables so both containers and the app agree on their values.

| Variable | Default | Notes |
|---|---|---|
| `POSTGRES_DB` / `_USER` / `_PASSWORD` | `payment_db` / `postgres` / `postgres` | Also used to build `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` and the `db-seed` job's `PGPASSWORD`, so the credential is set in one place. |
| `JWT_SECRET` | insecure placeholder | Must be set to a random value of at least 32 characters in any shared environment. See [SECURITY.md](SECURITY.md). |
| `JWT_EXPIRATION` | `3600000` (1 h) | Access token lifetime in milliseconds; refresh tokens live 7x longer. |
| `DB_RESET_ON_STARTUP` | `true` | Flyway cleans and re-migrates the schema on every boot. Set to `false` to keep data across restarts. |
| `MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED` | `true` | Enables the `/actuator/prometheus` exporter. |
| `GRAFANA_ADMIN_USER` / `_PASSWORD` | `admin` / `admin` | Grafana login (`http://localhost:3000`). |

`SPRING_KAFKA_BOOTSTRAP_SERVERS` (`kafka:9092`) and `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` (`http://otel-collector:9411/api/v2/spans`) are set directly in `docker-compose.yml` rather than through `.env`. Both are Docker-network-only hostnames that would break Kafka client construction with unresolvable bootstrap URLs if `.env` were ever sourced into a shell running `mvn test` on the host — keep them out of `.env` regardless of how it's loaded. The Zipkin endpoint points at `otel-collector` rather than `jaeger` directly so spans also feed the Service Performance Monitor pipeline (see below); the collector forwards every span on to Jaeger unchanged.

### Database reset behavior

`app.database.reset-on-startup=true` exists so local runs always match the checked-in migrations and stale dev data cannot mask a broken migration. It only works because `spring.flyway.clean-disabled` is set to `false` in `application.yml`. These two settings are the most dangerous ones in the project:

- Pointing this configuration at a non-disposable database will drop every object in it on the next boot.
- Before connecting to any shared or production database, set `DB_RESET_ON_STARTUP=false` **and** flip `spring.flyway.clean-disabled` back to `true` so a clean cannot happen even by misconfiguration.

With reset disabled, Flyway applies only pending migrations on startup. New schema changes are always a new `Vnnn__description.sql` file, never an edit to an existing one (Flyway checksums fail otherwise).

## Health and Verification

```bash
curl http://localhost:8080/actuator/health      # overall health
curl http://localhost:8080/actuator/prometheus  # metrics
open http://localhost:8080/swagger-ui.html      # API docs
open http://localhost:9090                      # Prometheus
open http://localhost:16686                     # Jaeger
open http://localhost:3000                      # Grafana (admin/admin by default)
```

Actuator exposes Kubernetes-style liveness and readiness probes (`management.endpoint.health.probes.enabled=true`), so `/actuator/health/liveness` and `/actuator/health/readiness` are ready to be wired into probe definitions when Kubernetes manifests are added to `k8s/`.

### Grafana Dashboard

`grafana/` holds everything needed for a working Grafana instance with zero manual setup:

```text
grafana/
├── provisioning/
│   ├── datasources/datasources.yml   # Prometheus (default) and Jaeger, with fixed uids
│   └── dashboards/dashboards.yml     # tells Grafana to auto-load ../../dashboards on startup
└── dashboards/
    ├── payment-core-overview.json    # application dashboard
    └── jaeger-spm.json               # tracing/SPM dashboard
```

The **Payment Core - Overview** dashboard (in the "Payment Core" folder) covers, in order: payment throughput and outcomes, payment/charge processing latency (P50/P95/P99), HTTP endpoint average latency, Resilience4j state (circuit breakers, retries, rate limiter permits, time limiter outcomes — see [docs/PERFORMANCE.md](PERFORMANCE.md)), infrastructure (HikariCP pool, JVM heap, GC pause, CPU), the Kafka producer/consumer pipeline, and links out to Jaeger for trace drill-down.

The **Jaeger - Service Performance Monitor** dashboard covers the span-derived RED metrics (request rate, error rate, P50/P95/P99 duration by operation, filterable via an `operation` template variable) plus Jaeger's own internal operational health (spans received/saved/rejected/dropped, collector queue depth, collector and storage latency) — see the Service Performance Monitor section below for where these come from.

Both dashboards are read-only from the Grafana UI (`allowUiUpdates: false` in `dashboards.yml`) so the JSON files are always the single source of truth — a saved UI edit would otherwise live only in Grafana's database and get silently reverted the next time the provisioner reconciles against the files. To change a panel, edit the relevant JSON file directly and either restart the `grafana` container or wait up to 30 s (`updateIntervalSeconds`) for it to reload automatically.

After a fresh startup, a quick smoke test is logging in with the seeded load test user (`load_test_user` / `LoadTest123!`) and creating a payment; `loadtest/README.md` documents this flow.

### Service Performance Monitor

Jaeger's UI has a **Monitor** tab (`http://localhost:16686/monitor`) showing request/error/duration metrics per service and operation, derived from span content rather than application metrics. This requires something to actually compute those metrics from spans — Jaeger's collector doesn't do this itself — so `otel-collector` sits between the app and Jaeger:

```text
payment-core --(zipkin)--> otel-collector --(zipkin, unchanged)--> jaeger
                                  |
                                  +--(spanmetrics connector)--> prometheus exporter :8889 --> prometheus
                                                                                                  |
                                                                                    jaeger queries <-+ (Monitor tab)
```

`otel-collector/otel-collector-config.yaml` defines the pipeline: a `zipkin` receiver accepts spans exactly as the app already sends them, a `zipkin` exporter forwards them to Jaeger unchanged, and the `spanmetrics` connector simultaneously derives `traces_span_metrics_calls_total` (a counter) and `traces_span_metrics_duration_milliseconds` (a histogram) per `service_name`/`span_name`/`span_kind`/`status_code`, exported for Prometheus to scrape at `:8889`.

The `jaeger` service reads those same metrics back via `METRICS_STORAGE_TYPE=prometheus` and `PROMETHEUS_SERVER_URL` (see `docker-compose.yml`) to power its own Monitor tab, independent of the Jaeger SPM Grafana dashboard querying the same Prometheus data directly.

`prometheus.yml` also scrapes `jaeger:14269/metrics` directly — Jaeger's own internal operational metrics (spans received/dropped/rejected, collector queue depth, storage latency), which are about the collector's own health rather than derived from span content, and power the "Jaeger Collector Health" and "Jaeger Storage" rows on the SPM dashboard.

## Production Checklist

The current setup is a development stack. Before any real deployment:

- Remove the JDWP agent (`-agentlib:jdwp=...`) from the Dockerfile entrypoint and stop publishing port 5005.
- Set `JWT_SECRET` from a secret store; never ship the default value.
- Set `DB_RESET_ON_STARTUP=false` and `spring.flyway.clean-disabled=true`.
- Replace the hardcoded `postgres/postgres` credentials with managed secrets.
- Do not run `db-seed` (it creates an account with published credentials).
- Restrict actuator exposure and Swagger UI; both are publicly reachable today (see [SECURITY.md](SECURITY.md)).
- Reduce tracing sampling from 1.0 to a production-appropriate rate.
- Kafka topics run with replication factor 1, which loses data if a single broker dies; raise it once a multi-broker cluster exists.
- Add resource limits and multiple app replicas; the app is stateless (JWT, no sessions) so it scales horizontally behind a load balancer.
