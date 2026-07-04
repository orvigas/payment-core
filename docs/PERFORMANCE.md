# Performance

Performance-relevant design choices, tuning values, and how to measure whether a change helped or hurt. Numbers below reflect the current configuration in `application.yml`, `KafkaConfig`, and `ResilienceConfig`; if you change one, update it here.

Related documents: [ARCHITECTURE.md](ARCHITECTURE.md), [DEPLOYMENT.md](DEPLOYMENT.md).

## Targets

From the load test suite (`loadtest/README.md`):

| Operation | p50 | p95 | p99 |
|---|---|---|---|
| Create payment (authenticated) | 100 ms | 500 ms | 1000 ms |

Steady-state runs are expected to hold p95 under 500 ms with an error rate below 5%; during spike tests the thresholds relax to p99 under 2 s and error rate below 20%.

## Database

### Connection pool (HikariCP)

- `maximum-pool-size: 20`, `minimum-idle: 5`. Raised from the default 10 because the load profile (50 concurrent virtual users plus Kafka consumers sharing the pool) exhausted the smaller pool.
- `connection-timeout: 20s`, `idle-timeout: 10m`, `max-lifetime: 30m`.
- Server-side prepared statement caching is enabled (`cachePreparedStatements`, cache size 250, SQL limit 2048) so hot queries skip re-parsing.

### Hibernate

- `jdbc.batch_size: 20` with `order_inserts` and `order_updates` enabled, so bursts of writes go out as batched statements instead of row-by-row round trips. Ordering also prevents foreign key violations when related rows are flushed together.
- `ddl-auto: validate` means Hibernate does no schema work at runtime; Flyway handles it once at startup.

### Indexes

Indexes exist only where queries actually hit: `paymentId` (PK lookups), `userId` (a user's payments), and `status` (state filtering). Adding a query pattern that scans on another column should come with a new index in a Flyway migration — and evidence from Jaeger or `EXPLAIN` that it is needed.

## Kafka

- Producer: `acks=all` (durability over latency for payment events), 3 retries, snappy compression to cut network payload size at low CPU cost.
- Topics have 3 partitions and listener containers run with concurrency 3, so consumer threads match partitions; raising throughput means raising both together, since extra consumer threads beyond the partition count sit idle.
- Consumers use typed deserializer factories per event class, which avoids reflective type resolution per message on top of the correctness benefits described in [ARCHITECTURE.md](ARCHITECTURE.md).

The payment flow is designed so the synchronous API path stays short: `POST /api/v1/payments` does one validated insert plus one produce, and all charging work happens in consumers. API latency is therefore mostly database plus produce time, and slow charging shows up as consumer lag rather than slow HTTP responses.

## Resilience Settings

Resilience4j protects the integration edges. The values differ per dependency on purpose:

| Concern | Setting | Reasoning |
|---|---|---|
| Charger circuit breaker | 50% failure threshold, 30 s open | The charger is on the money path; fail fast and give it time to recover rather than queueing doomed calls. |
| Notification circuit breaker | 60% failure threshold, 20 s open | Notifications are best-effort; tolerate more failures before opening. |
| Charger retry | 3 attempts, exponential backoff from 100 ms, capped at 1 s | Covers transient network errors without stretching a user-facing call. |
| Kafka retry | 5 attempts, randomized backoff | Broker hiccups are common and self-healing; jitter avoids a thundering herd of synchronized retries. |
| Charger timeout | 5 s | External call; bounded so threads are not held hostage. |
| Database timeout | 2 s | Local dependency; anything slower than this is already an incident. |
| Slow-call detection | Calls over 2 s count as failures (50% slow-call threshold) | A dependency that is technically up but consistently slow should still open the breaker. |

Rate limiters (10 logins/min, 100 payment creations/hour) exist for abuse protection rather than throughput shaping; they are documented in [SECURITY.md](SECURITY.md).

## Load Testing

The k6 suite in `loadtest/` is the standard way to validate a performance-relevant change. Full setup, seeding, and troubleshooting are in `loadtest/README.md`; the short version:

```bash
docker-compose up -d              # db-seed creates the load test user automatically
cd loadtest
k6 run payment-load-test.js       # login + authenticated payment creation, 10-50 VUs over 9 min
k6 run scenarios/steady-state.js  # 50 VUs for 10 min
k6 run scenarios/spike-test.js    # 50 -> 500 VUs spike and recovery
k6 run payment-load-test.js --out json=results/test-run.json
```

Run a baseline before the change and the same scenario after it, on the same machine, and compare p95/p99 and error rate. Historical results and past optimizations are tracked in `loadtest/IMPROVEMENTS.md`.

## Measuring

- **Prometheus** (`http://localhost:9090`) scrapes `/actuator/prometheus` every 15 s. The custom metrics that matter for latency work: `payment.processing.duration` and `charge.processing.duration` (both publish P50/P95/P99), plus the `payment.*` and `charge.*` counters for throughput and failure rates.
- **Resilience4j metrics** (`resilience4j_circuitbreaker_state`, `resilience4j_circuitbreaker_failure_rate`, `resilience4j_retry_calls_total`, `resilience4j_ratelimiter_available_permissions`, `resilience4j_timelimiter_calls_total`) show the actual behavior of the thresholds in the table above — e.g. whether `payment-creation` is close to exhausting its 100/hour budget, or whether `charger-service` is tripping open. The registries in `ResilienceConfig`/`RateLimitingConfig` are created manually rather than through Spring Boot's Resilience4j autoconfiguration, so their metrics are bound explicitly via `Tagged*Metrics.bindTo(meterRegistry)` rather than appearing for free.
- **Jaeger** (`http://localhost:16686`) shows where time goes inside a request; use it before guessing at a bottleneck. Sampling is 100% locally, which is itself a cost — reduce `management.tracing.sampling.probability` in production.
- **Consumer lag** is the early warning for the async path: payments stuck in `PENDING`/`PROCESSING` usually mean lagging consumers, not a slow API.

When adding a new operation, register its counter and timer in `CustomMetrics` up front. Retrofitting metrics after a performance question arrives is always worse than having the history.
