# Error Test Scenario - Loki & Jaeger Validation

## Overview

The `error-test.js` scenario deliberately generates various HTTP error conditions to validate log aggregation (Loki) and distributed tracing (Jaeger).

**Error types tested:**
- `401 Unauthorized` - Missing/invalid authentication tokens
- `400 Bad Request` - Invalid payment data (missing amount, negative values, invalid currency)
- `404 Not Found` - Accessing non-existent resources
- `429 Too Many Requests` - Rate limiting (rapid login attempts)
- `201 Created` - Successful payments (for comparison)

## Prerequisites

Ensure the observability stack is running:

```bash
docker-compose up -d
```

Verify services are healthy:
- Loki: http://localhost:3100/ready
- Jaeger: http://localhost:16686
- Grafana: http://localhost:3000
- App: http://localhost:8080/actuator/health

## Running the Test

### Basic Run (3 minutes, 10 concurrent users)

```bash
cd loadtest
k6 run loadtest/scenarios/error-test.js
```

### With Custom Options

```bash
# Run for 5 minutes with 20 users
k6 run -u 20 -d 5m loadtest/scenarios/error-test.js

# Run with verbose logging
k6 run -v loadtest/scenarios/error-test.js

# Output results to JSON for analysis
k6 run loadtest/scenarios/error-test.js --out json=loadtest/results/error-test-run.json
```

## What to Expect

### Loki Dashboard (`loki-logs-detailed`)

After running the test, open: http://localhost:3000/d/loki-logs-detailed

**Expected log patterns:**

1. **Error Rate (stat tile)** - Should show non-zero errors (5-15% of total logs)

2. **Recent Error Logs (table)**
   - Multiple 401 Unauthorized logs (missing/invalid tokens)
   - Multiple 400 Bad Request logs (validation failures)
   - 404 Not Found logs (non-existent payment queries)
   - 429 Too Many Requests logs (rate limit hits)

3. **Log Distribution by Level (pie chart)**
   - ERROR: ~10-20% of logs
   - WARN: Minor portion
   - INFO/DEBUG: Bulk of logs

4. **Log Count by Level** (1m rolling chart)
   - Visible spikes during error injection periods

### Sample Error Logs in Loki

```json
{
  "timestamp_ms": 1783223883465,
  "logger_name": "org.springframework.web.servlet.DispatcherServlet",
  "level": "ERROR",
  "message": "Completed 401 Unauthorized",
  "mdc_traceId": "6a49d64aa93274bcea1b21d8b677cf10",
  "mdc_spanId": "3b3b35d8b338864b"
}
```

### Jaeger Tracing (`jaeger-spm`)

After running the test, open: http://localhost:16686

**Expected traces:**

1. **Failed Operations (red spans)**
   - Auth failures: POST /api/v1/auth/login with 401
   - Payment validation errors: POST /api/v1/payments with 400
   - Not found: GET /api/v1/payments/{id} with 404

2. **Trace Waterfall**
   - Each error request should have a trace showing the full stack
   - Look for controller → service → database layers
   - Error details in span tags/logs

3. **Service Performance Monitor**
   - "payment-core" service shows:
     - Total requests including errors
     - P50/P95/P99 latencies
     - Error rate by operation

### Custom Metrics in K6 Output

The test tracks:
- `auth_errors` - Count of 401 responses
- `validation_errors` - Count of 400 responses
- `not_found_errors` - Count of 404 responses
- `rate_limit_errors` - Count of 429 responses
- `error_rate` - Overall error rate

Example output:
```
     checks.........................: 97.19% ✓ 628 ✗ 18
     error_rate......................: 15.23%  ✓ 0
     auth_errors.....................: 85
     validation_errors...............: 150
     not_found_errors................: 20
     rate_limit_errors...............: 15
```

## Analyzing Results

### Step 1: Run the Error Test

```bash
cd loadtest
k6 run loadtest/scenarios/error-test.js
```

Take note of the start time (shows in console output or use `date +%s`).

### Step 2: Open Loki Dashboard

Navigate to: http://localhost:3000/d/loki-logs-detailed

**Time picker**: Set to "Last 10 minutes" to capture the test window.

**Query errors**:
```
{app="payment-core", level="ERROR"}
```

### Step 3: Open Jaeger Dashboard

Navigate to: http://localhost:16686

**Filter by service**: "payment-core"

**Filter by tags** (in Jaeger):
- `http.status_code=401` - Auth failures
- `http.status_code=400` - Validation errors
- `http.status_code=404` - Not found
- `http.status_code=429` - Rate limits

### Step 4: Cross-Reference

1. **Find an error log in Loki**
   - Copy the `mdc_traceId` from the log
   
2. **Search that trace in Jaeger**
   - Paste traceId in Jaeger search box
   - Verify the trace shows the full request flow
   - Check for error context in span logs/tags

## Common Issues

### No Errors Showing in Loki

**Symptom**: Error test ran but Loki shows only INFO/DEBUG logs.

**Cause**: Logs haven't flushed to Loki yet (batched ingestion).

**Fix**: 
- Wait 10-15 seconds after test completes
- Refresh Loki dashboard
- Check app logs: `docker-compose logs payment-core | grep ERROR`

### Jaeger Shows No Traces

**Symptom**: No traces appear for the test time window.

**Cause**: Tracing not enabled or trace propagation broken.

**Fix**:
- Verify app is sending spans: `docker-compose logs payment-core | grep jaeger`
- Check MANAGEMENT_ZIPKIN_TRACING_ENDPOINT is set: `docker-compose exec payment-core env | grep ZIPKIN`
- Verify otel-collector is receiving spans: `docker-compose logs otel-collector | grep spans`

### Rate Limit Not Triggering

**Symptom**: Error test expects 429 but gets 401 instead.

**Reason**: Rate limit per-endpoint not yet hit (would need even faster requests).

**Workaround**: Increase loop iterations in Test 7 or reduce sleep times.

## Test Variations

### Heavy Error Load (for stress testing)

```bash
k6 run -u 50 -d 10m loadtest/scenarios/error-test.js
```

Expected: 20-30% error rate, visible in Loki error trend charts.

### Single VU (for detailed trace analysis)

```bash
k6 run -u 1 -d 2m loadtest/scenarios/error-test.js
```

Expected: Easier to correlate individual requests in Jaeger.

### Production-like (errors mixed with success)

Run both scenarios concurrently:
```bash
# Terminal 1: Normal load
k6 run payment-load-test.js &

# Terminal 2: Error test
k6 run loadtest/scenarios/error-test.js
```

Observe: Error rate stays low in metrics but visible in logs/traces.

## What You'll Learn

1. **Log aggregation**: How errors appear in Loki with proper labels and context
2. **Distributed tracing**: How traces correlate logs to request flow across services
3. **Observability**: Relationship between metrics (K6), logs (Loki), and traces (Jaeger)
4. **Dashboard design**: Loki dashboard effectively surfacing errors for operators

## Next Steps

After validating the dashboards:
1. Tune Loki retention and ingestion rates for your volume
2. Set up alerting rules in Grafana based on error thresholds
3. Create additional dashboards for specific services or flows
4. Document runbooks for common errors and how to troubleshoot them
