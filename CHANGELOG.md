# Changelog

Notable changes to this project. Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

Nothing yet.

## [1.1.2] - 2026-07-05

### Fixed

- **Broken Docker build:** `Dockerfile`'s final `COPY --from=builder` stage hardcoded `payment-core-1.0.0.jar`, a filename that hasn't existed since the `pom.xml` version moved to 1.1.0. Every `docker build` since then failed at that step with a missing-file error. Replaced the hardcoded filename with a glob, `payment-core-*.jar`, so the build no longer needs updating on every version bump. Spring Boot's repackage plugin also leaves a `payment-core-<version>.jar.original` file in `target/`, but that name ends in `.jar.original` rather than `.jar`, so the glob still resolves to a single match.

## [1.1.1] - 2026-07-05

### Documentation & Accuracy

**Why this patch:** v1.1 was released with stale or missing documentation that contradicted the actual project, potentially confusing users or causing setup failures. This patch corrects all drift without code changes. **No functionality changes; code is identical to v1.1.**

### Fixed

- **Dependency version accuracy:** Fixed stale version numbers in docs. README and CLAUDE.md incorrectly stated "Spring Kafka 3.1.2" and "Spring Boot 3.5.0", but pom.xml pins 3.3.16 and 3.5.16 respectively. This mismatch could confuse troubleshooting and compatibility decisions.
- **Project version drift:** `pom.xml`'s own `<version>` tag was still 1.0.0 while the git repository was at v1.1; now correctly set to 1.1.1.
- **Incomplete CHANGELOG:** v1.0 release was missing (backfilled from GitHub release with 2026-07-03 date), v1.1 entry was incomplete (missing the `.env.example` scrub and `.gitignore` entry for loadtest/results). Both now complete with links.
- **Broken CI/codecov badges:** README had Build Status and Code Coverage badges that referenced nonexistent infrastructure (no `.github/workflows/` dir, no codecov config). Replaced with a real GitHub release badge pointing to the actual release page.

### Documentation Changes

- **README.md:** Updated Spring Kafka version 3.1.2 → 3.3.16, added CHANGELOG to the Documentation index, replaced broken badges.
- **CHANGELOG.md:** Added [1.0] section (2026-07-03) and [1.1] section (2026-07-05) with complete entries and GitHub compare links, created this [1.1.1] patch section explaining the rationale.
- **CLAUDE.md:** Updated Spring Boot 3.5.0 → 3.5.16, Spring Kafka 3.1.2 → 3.3.16, added loki-logback-appender and logstash-logback-encoder to the Dependencies & Versions list (missing from initial v1.1 docs).

### Notes

No code changes; all files modified are documentation, configuration metadata, or changelog. No functional behavior, dependencies, or database schema changes. Users on v1.1 do not need to upgrade unless they want the corrected documentation. Automated tooling (dependency audits, version checks, CI setup guides) now has accurate information.

## [1.1] - 2026-07-05

### Added

- Grafana Loki for centralized log aggregation: `loki` service in `docker-compose.yml`, a Loki datasource in Grafana, and two dashboards (`loki-logs.json`, `loki-logs-detailed.json`) covering log volume, error rate, and per-level drill-down tables.
- `logback-spring.xml`: structured JSON logs shipped to Loki via `loki-logback-appender`, alongside the existing console and rolling-file appenders, with per-package log levels and per-profile roots (`dev`/`test`/`prod`).
- `k6` error-test scenario (`loadtest/scenarios/error-test.js`) that drives 401/400/404/429 responses to populate the Loki and Jaeger dashboards for validation, plus `loadtest/QUICK_START.md` and `loadtest/ERROR_TEST_README.md`.
- Grafana dashboard screenshots in the README's Observability section.

### Changed

- `GlobalExceptionHandler.buildErrorResponse` enriches error logs with `http_status_code` and `error_message` via MDC, so error responses are queryable by status code in Loki.
- Bumped `logstash-logback-encoder` from 7.3 to 9.0.
- Updated `README.md`, `docs/DEPLOYMENT.md`, and `docs/ARCHITECTURE.md` to describe the Loki/Grafana logging pipeline and its startup ordering.
- `.env.example` no longer ships realistic-looking credential defaults (`postgres`, a fake JWT string, `admin`, `LoadTest123!`); every credential-shaped variable is now a generic `CHANGEME` placeholder, with the actual conventional local-dev values documented in README.md's Configuration section instead.

### Fixed

- MDC context set by `GlobalExceptionHandler` is now cleared in a `finally` block, preventing `http_status_code`/`error_message` from leaking into log lines of unrelated requests on a reused Tomcat thread.

### Security

- Lowered `org.hibernate.type.descriptor.sql.BasicBinder` from `TRACE` to `WARN` so SQL bind parameter values (payment amounts, user IDs) are not shipped to Loki.
- Removed secret-shaped defaults from `.env.example` that were tripping GitHub secret scanning, even though none of them were real secrets.

### Removed

- Unused `gson` dependency from `pom.xml`.

### Chore

- Added `loadtest/results/` to `.gitignore`; generated k6 output should not be committed.

## [1.0] - 2026-07-03

Initial release. Complete backend payment processing system with event-driven architecture, observability, and production-grade testing.

### Added

- REST API for payment operations (create, retrieve, confirm, refund) under `/api/v1/payments`, plus JWT auth (`/api/v1/auth`) with access/refresh tokens and BCrypt password storage.
- Rate limiting on login and payment creation via Resilience4j.
- Event-driven processing with Apache Kafka: charging, notifications, and analytics consumers.
- PostgreSQL with Flyway-managed schema migrations (V001-V008) and strategic indexing.
- Resilience patterns: circuit breakers, retries, and timeouts around the charger, notifications, Kafka, and database calls.
- Observability: Prometheus metrics, Jaeger distributed tracing (with a Service Performance Monitor dashboard via a companion OpenTelemetry Collector), custom Micrometer metrics, and an auto-provisioned Grafana dashboard.
- Interactive API documentation with Swagger UI / OpenAPI 3.1.
- k6 load testing suite (progressive ramp, steady-state, spike, and wave scenarios) with seeded credentials.
- Around 259 unit and integration tests, with a 95% instruction coverage minimum enforced via JaCoCo.

[Unreleased]: https://github.com/orvigas/payment-core/compare/v1.1.2...HEAD
[1.1.2]: https://github.com/orvigas/payment-core/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/orvigas/payment-core/compare/v1.1...v1.1.1
[1.1]: https://github.com/orvigas/payment-core/compare/v1.0...v1.1
[1.0]: https://github.com/orvigas/payment-core/releases/tag/v1.0
