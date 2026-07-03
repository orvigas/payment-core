# Payment Core Load Tests

Comprehensive load testing suite for the Payment Core API using k6.

## Overview

This directory contains load tests for validating payment API performance, scalability, and reliability under various conditions:

- **Full Flow Test**: Complete user journey (create payment → get payment) with progressive load
- **Steady State Test**: Sustained load to validate long-running stability
- **Spike Test**: Traffic spike to test resilience and recovery

## Test Scripts

### payment-load-test.js

Complete workflow simulating real user behavior:

1. Create payment (POST /api/v1/payments)
2. Wait 1 second
3. Retrieve payment (GET /api/v1/payments/{id})
4. Random wait (0-3 seconds)

**Load Profile**:
- Ramps from 10 to 100 users over 19 minutes
- Validates 95th percentile latency < 500ms for create, < 200ms for get
- Enforces < 10% error rate

### scenarios/steady-state.js

Sustained load test to validate stable operation:

- 50 concurrent users
- 10-minute duration
- Creates payments at constant rate
- Validates sustained performance (p95 < 500ms, error rate < 5%)

### scenarios/spike-test.js

Traffic spike scenario to test resilience:

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

All tests track:

| Metric | Type | Description |
|--------|------|-------------|
| `http_req_duration` | Trend (ms) | Request latency |
| `create_payment_duration_ms` | Trend | Create operation latency |
| `get_payment_duration_ms` | Trend | Get operation latency |
| `http_req_failed` | Rate | Failed request percentage |
| `errors` | Rate | Custom error rate |
| `payments_success` | Counter | Successful payments |
| `payments_failed` | Counter | Failed payments |

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

Thresholds define pass/fail criteria for tests:

### Full Flow Test
- Create: p95 < 500ms, p99 < 1000ms
- Get: p95 < 200ms, p99 < 500ms
- Error rate: < 10%

### Steady State
- Create: p95 < 500ms, p99 < 1000ms
- Error rate: < 5%

### Spike Test
- Create: p99 < 2000ms (relaxed due to spike)
- Error rate: < 20% (relaxed due to spike)

## Test Design Considerations

### Cardinality Management

These tests use deterministic user ID pooling to avoid metrics cardinality explosion:

- User IDs are pooled, not random
- Request metrics are grouped by operation, not ID
- Each endpoint tagged with `name` for clear separation
- Reduces metric cardinality from 200k+ to ~10 series

### Realistic Behavior

- Progressive load ramp (not instant)
- Think time between operations (1-3 seconds)
- Varied amounts and merchants (realistic variance)
- Payment flow tests create then retrieve (common pattern)

### Performance Validation

Tests validate critical paths:

1. **Latency**: Response times within acceptable bounds
2. **Success Rate**: Error rate below threshold
3. **Recovery**: System recovers after load spikes

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

For reference, target metrics:

| Operation | p50 | p95 | p99 |
|-----------|-----|-----|-----|
| Create Payment | 100ms | 450ms | 800ms |
| Get Payment | 50ms | 150ms | 300ms |

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

Add custom tracking:

```javascript
// In test files, add payload metrics
const paymentAmountHistogram = new Trend('payment_amount');
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
