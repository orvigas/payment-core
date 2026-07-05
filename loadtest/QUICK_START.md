# Quick Start - K6 Load Testing

## One-Command Validation

Test Grafana Loki & Jaeger with error scenarios:

```bash
# Ensure stack is running
docker-compose up -d

# Run error test (generates diverse HTTP errors for tracing)
cd loadtest
k6 run scenarios/error-test.js

# While running, open in another terminal:
# Loki: http://localhost:3000/d/loki-logs-detailed
# Jaeger: http://localhost:16686
```

## Available Scenarios

| Scenario | Purpose | Duration | VUs | Run Command |
|----------|---------|----------|-----|-------------|
| **payment-load-test.js** | Progressive ramp, normal load | 12 min | 10→50 | `k6 run payment-load-test.js` |
| **error-test.js** | Error conditions for observability | 3 min | 10 | `k6 run scenarios/error-test.js` |
| **steady-state.js** | Constant load, stability validation | 10 min | 50 | `k6 run scenarios/steady-state.js` |
| **spike-test.js** | Traffic spike & recovery | 8 min | 50→500 | `k6 run scenarios/spike-test.js` |
| **wave-test.js** | 100 individual user auth flows | 10 min | 100 | `k6 run scenarios/wave-test.js` |

## For Observability Testing

### Step 1: Start Observability Stack
```bash
docker-compose up -d loki grafana prometheus jaeger payment-core
```

### Step 2: Run Error Test
```bash
cd loadtest
k6 run scenarios/error-test.js
```

### Step 3: Monitor in Real-time

**Loki Dashboard** (Log Aggregation)
- Open: http://localhost:3000/d/loki-logs-detailed
- Look for:
  - "Errors (5m)" stat showing 50-150+ errors
  - "Recent Error Logs" table with 401/400/404 responses
  - Error trends in charts

**Jaeger Dashboard** (Distributed Tracing)
- Open: http://localhost:16686
- Filter by service: "payment-core"
- Look for:
  - Failed requests (red traces)
  - Error status codes in spans
  - Correlation between logs and traces

### Step 4: Analyze Results

Take screenshots:
1. Loki error log table with timestamps
2. Jaeger trace waterfall showing the error flow
3. Compare error patterns across both systems

## Result Files

Test output saved to: `results/` directory

```bash
# Save JSON results for analysis
k6 run scenarios/error-test.js --out json=results/error-test-$(date +%s).json

# View summary
k6 run scenarios/error-test.js --summary-export=results/summary.json
```

## Common Commands

```bash
# Run with custom VU count and duration
k6 run -u 20 -d 5m scenarios/error-test.js

# Verbose output (show all logs)
k6 run -v scenarios/error-test.js

# Run multiple iterations
k6 run --iterations 100 scenarios/error-test.js

# Set environment variables
BASE_URL=http://api.example.com k6 run scenarios/error-test.js
```

## Troubleshooting

### "Connection refused"
- Ensure docker-compose services are running: `docker-compose ps`
- Wait for app to be healthy: `curl http://localhost:8080/actuator/health`

### "Login failed" in k6
- Verify seed script has run: `docker-compose logs db-seed`
- Check load test user exists: 
  ```bash
  docker-compose exec postgres psql -U postgres -d payment_db -c "SELECT * FROM users WHERE username='load_test_user';"
  ```

### No logs in Loki
- Wait 10-15 seconds (batch ingestion)
- Refresh Grafana dashboard
- Check app logs: `docker-compose logs payment-core | grep ERROR`

### No traces in Jaeger
- Verify otel-collector is healthy: `docker-compose ps otel-collector`
- Check traces are being exported: `docker-compose logs payment-core | grep jaeger`

## Next Steps

After validating observability:
1. Read `ERROR_TEST_README.md` for detailed error scenario documentation
2. Customize error scenarios for your use cases
3. Create dashboards for your specific metrics
4. Set up alerting rules in Grafana
