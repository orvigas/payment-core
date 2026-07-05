# Changelog

Notable changes to this project. Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- Grafana Loki for centralized log aggregation: `loki` service in `docker-compose.yml`, a Loki datasource in Grafana, and two dashboards (`loki-logs.json`, `loki-logs-detailed.json`) covering log volume, error rate, and per-level drill-down tables.
- `logback-spring.xml`: structured JSON logs shipped to Loki via `loki-logback-appender`, alongside the existing console and rolling-file appenders, with per-package log levels and per-profile roots (`dev`/`test`/`prod`).
- `k6` error-test scenario (`loadtest/scenarios/error-test.js`) that drives 401/400/404/429 responses to populate the Loki and Jaeger dashboards for validation, plus `loadtest/QUICK_START.md` and `loadtest/ERROR_TEST_README.md`.
- Grafana dashboard screenshots in the README's Observability section.

### Changed

- `GlobalExceptionHandler.buildErrorResponse` enriches error logs with `http_status_code` and `error_message` via MDC, so error responses are queryable by status code in Loki.
- Bumped `logstash-logback-encoder` from 7.3 to 9.0.
- Updated `README.md`, `docs/DEPLOYMENT.md`, and `docs/ARCHITECTURE.md` to describe the Loki/Grafana logging pipeline and its startup ordering.

### Fixed

- MDC context set by `GlobalExceptionHandler` is now cleared in a `finally` block, preventing `http_status_code`/`error_message` from leaking into log lines of unrelated requests on a reused Tomcat thread.

### Security

- Lowered `org.hibernate.type.descriptor.sql.BasicBinder` from `TRACE` to `WARN` so SQL bind parameter values (payment amounts, user IDs) are not shipped to Loki.

### Removed

- Unused `gson` dependency from `pom.xml`.

### Chore

- Added `loadtest/results/` to `.gitignore`; generated k6 output should not be committed.
