# Payment Core Load Tests

Comprehensive load testing suite for the Payment Core API using k6.

## Overview

This directory contains load tests for validating payment API performance, scalability, and reliability under various conditions:

- **Full Flow Test**: Complete user journey (create payment → get payment) with progressive load
- **Steady State Test**: Sustained load to validate long-running stability
- **Spike Test**: Traffic spike to test resilience and recovery

## Test Scripts

### payment-load-test.js

JWT-authenticated payment creation workflow:

1. Setup phase: Obtain JWT access token via login
2. Load phase: Create payments with JWT authentication (POST /api/v1/payments)

**Load Profile**:
- Ramps from 10 to 50 users over 9 minutes (simplified for focused testing)
- Tests authenticated endpoints with Bearer token in Authorization header

### scenarios/steady-state.js

Sustained load test to validate stable operation. Like `payment-load-test.js`, it logs in once in the setup phase and sends the resulting Bearer token on every request.

- 50 concurrent users
- 10-minute duration
- Creates payments at constant rate
- Validates sustained performance (p95 < 500ms, error rate < 5%)

### scenarios/spike-test.js

Traffic spike scenario to test resilience. Like `payment-load-test.js`, it logs in once in the setup phase and sends the resulting Bearer token on every request.

- Normal load (50 users for 2 minutes)
- Spike to 500 users (1 minute)
- Sustained spike (2 minutes)
- Cool down (1 minute)
- Relaxed thresholds during spike (p99 < 2s, error rate < 20%)

## Installation

```bash
# Install k6 (macOS)
brew install k6

# Verify installation
k6 version
```

## Running Tests

### Seed the Load Test User

`payment-load-test.js` logs in with real credentials, and there's no registration endpoint, so
the account has to exist in the database before the first run.

If you're running the stack via `docker-compose up`, this is automatic: the `db-seed` service
reruns `seed-load-test-user.sql` after every startup, since `app.database.reset-on-startup`
(the default) has Flyway wipe the `users` table on every app boot.

If you're running the app some other way (e.g. `mvn spring-boot:run` against a local Postgres),
seed it manually:

```bash
psql -h localhost -U postgres -d payment_db -f seed-load-test-user.sql
```

Override `LOAD_TEST_USERNAME`/`LOAD_TEST_PASSWORD` when invoking k6 if you seed different
credentials.

### Basic Usage

```bash
# Run full flow test
k6 run payment-load-test.js

# Run steady state scenario
k6 run scenarios/steady-state.js

# Run spike test scenario
k6 run scenarios/spike-test.js
```

### Custom Configuration

```bash
# Custom base URL
BASE_URL=http://api.example.com k6 run payment-load-test.js

# Local development
BASE_URL=http://localhost:8080 k6 run payment-load-test.js
```

### Output Formats

```bash
# Generate JSON output for analysis
k6 run payment-load-test.js --out json=results/full-flow.json

# Generate CSV metrics
k6 run payment-load-test.js --out csv=results/metrics.csv

# Compare against baseline
k6 run payment-load-test.js --out json=results/latest.json

# View HTML summary (requires k6-reporter plugin)
k6 run payment-load-test.js --out json=results/output.json && \
  k6 run -e ENV=prod scenarios/steady-state.js
```

## Metrics

The test tracks standard k6 metrics:

| Metric | Type | Description |
|--------|------|-------------|
| `http_req_duration` | Trend (ms) | Request latency |
| `http_req_failed` | Rate | Failed request percentage |
| `checks` | Rate | Custom check pass rate |
| `data_received` | Counter | Total bytes received |
| `data_sent` | Counter | Total bytes sent |

### Viewing Results

k6 outputs results in real-time to stdout. Look for:

```
checks.........................: 98.50% ✓ 394 ✗ 6
data_received...................: 236 kB
data_sent........................: 118 kB
http_req_duration................: avg=142ms min=12ms med=98ms max=1.2s p(90)=320ms p(95)=450ms p(99)=800ms
http_req_failed..................: 1.50%
payments_success..................: 197
payments_failed..................: 3
```

## Thresholds

The main test focuses on validating successful operations:

### payment-load-test.js
- All requests should return 201 (Created) status
- All responses should include a paymentId
- Error rate should be minimal

Thresholds can be customized by modifying the `options` object in the test file.

## Test Design Considerations

### Authentication Testing

All three scripts (`payment-load-test.js`, `scenarios/steady-state.js`, `scenarios/spike-test.js`) share the same authentication setup:

- Single JWT token obtained in setup phase via login endpoint
- Token shared across all load test iterations and VUs
- Authorization header includes Bearer token for each payment creation request

### Load Testing Focus

- Progressive load ramp (not instant spike)
- Tests authenticated endpoints under realistic user count
- Validates both successful operations and error handling

### Performance Validation

Tests validate:

1. **Success Rate**: Payments created successfully with valid JWT auth
2. **Response Validity**: All responses include paymentId
3. **Load Stability**: System handles increased concurrent users

## Baseline Comparison

Baseline results stored in `results/baseline.json`:

```bash
# Run test and compare against baseline
k6 run payment-load-test.js -e BASELINE=true
```

See `IMPROVEMENTS.md` for historical improvements and optimizations.

## Troubleshooting

### High Cardinality Warning

If you see:
```
WARN: metrics with high cardinality detected...
```

This is resolved by request tagging. Ensure tests use `tags: { name: 'operation_name' }` on all requests.

### Connection Refused

Check application is running:
```bash
curl http://localhost:8080/actuator/health
```

### Timeout Errors

- Increase VU ramp-up time in test config
- Check database performance
- Monitor Kafka lag if event processing is slow

### Memory Issues

If k6 process uses excessive memory:
- Reduce number of concurrent VUs
- Reduce test duration
- Check for memory leaks in monitored application

## CI/CD Integration

Run in CI pipeline:

```bash
# In GitHub Actions
- name: Load Test
  run: |
    k6 run payment-load-test.js \
      --out json=results/load-test.json \
      --threshold 'http_req_failed{method:POST}:rate<0.1'
```

Fail CI if thresholds not met:

```bash
k6 run payment-load-test.js || exit 1
```

## Performance Targets

For reference, expected performance metrics (with JWT authentication):

| Operation | p50 | p95 | p99 |
|-----------|-----|-----|-----|
| Create Payment (Authenticated) | 100ms | 500ms | 1000ms |

## Extensions

### Virtual Users by Scenario

Adjust VU count for different environments:

```bash
# Light load (development)
k6 run payment-load-test.js --vus 5 --duration 2m

# Medium load (staging)
k6 run payment-load-test.js --vus 50 --duration 10m

# Heavy load (production verification)
k6 run payment-load-test.js --vus 500 --duration 30m
```

### Custom Metrics

The current test focuses on standard k6 metrics. To add custom tracking:

```javascript
// In test files, import metrics
import { Trend } from 'k6/metrics';

// Create custom metric
const paymentAmountHistogram = new Trend('payment_amount');

// Add values during test execution
paymentAmountHistogram.add(payload.amount);
```

## References

- [k6 Documentation](https://grafana.com/docs/k6/latest/)
- [k6 Scripting API](https://grafana.com/docs/k6/latest/javascript-api/)
- [REST API Tests](https://grafana.com/docs/k6/latest/examples/rest-api-test/)
- [Thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)

## Maintenance

- Review and update thresholds quarterly based on production metrics
- Keep VU profiles aligned with expected peak loads
- Archive baseline results for historical comparison
- Update test scenarios when API contracts change
