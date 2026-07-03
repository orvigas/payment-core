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
| `prometheus` | prom/prometheus | 9090 | Metrics scraping and dashboard |
| `jaeger` | jaegertracing/all-in-one | 16686 (UI), 9411 (collector) | Distributed tracing |
| `payment-core` | built from `Dockerfile` | 8080 (HTTP), 5005 (debug) | The application |
| `db-seed` | postgres:15-alpine | - | Seeds the load test user, then exits |

All services share the `payment-network` bridge network. Postgres and Prometheus data live in named volumes (`postgres_data`, `prometheus_data`) and survive `docker-compose down`; use `down -v` to wipe them.

### Startup ordering

Compose enforces a healthcheck chain rather than plain start ordering:

1. `postgres`, `zookeeper`, `prometheus`, `jaeger` become healthy.
2. `kafka` starts after `zookeeper` is healthy.
3. `payment-core` starts after `postgres`, `kafka`, `prometheus`, and `jaeger` are all healthy, and is itself considered healthy once `/actuator/health` responds.
4. `db-seed` runs `loadtest/seed-load-test-user.sql` only after `payment-core` is healthy.

The seed job waits on the application rather than on Postgres because the app's Flyway run is what creates the `users` and `user_roles` tables — seeding after Postgres alone would race the migrations. It also reruns on every startup, not just the first, because the default `DB_RESET_ON_STARTUP=true` wipes the schema on each boot (see below).

## Configuration

Configuration lives in `src/main/resources/application.yml` and is overridden through environment variables. The compose file already overrides the datasource, Kafka, and tracing endpoints to point at the container network.

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | insecure placeholder | Must be set to a random value of at least 32 characters in any shared environment. See [SECURITY.md](SECURITY.md). |
| `JWT_EXPIRATION` | `3600000` (1 h) | Access token lifetime in milliseconds; refresh tokens live 7x longer. |
| `DB_RESET_ON_STARTUP` | `true` | Flyway cleans and re-migrates the schema on every boot. Set to `false` to keep data across restarts. |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | localhost Postgres | Database connection. |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers. |
| `SPRING_ZIPKIN_BASE_URL` | `http://localhost:9411` | Jaeger collector (Zipkin protocol). |

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
```

Actuator exposes Kubernetes-style liveness and readiness probes (`management.endpoint.health.probes.enabled=true`), so `/actuator/health/liveness` and `/actuator/health/readiness` are ready to be wired into probe definitions when Kubernetes manifests are added to `k8s/`.

After a fresh startup, a quick smoke test is logging in with the seeded load test user (`load_test_user` / `LoadTest123!`) and creating a payment; `loadtest/README.md` documents this flow.

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
