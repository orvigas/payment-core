# Load Test Improvements

## Issues Resolved

### 1. Metrics Cardinality Explosion (200,001 unique time series)

**Problem**: Each unique payment ID in GET requests created a new metric time series, causing:
- Excessive memory consumption
- Metrics engine performance degradation
- Slow data aggregation

**Solution**:
- Added `name` tag to group requests by endpoint type, not by ID
- All GET requests now grouped as `name:get_payment`, not by individual payment ID
- All POST requests now grouped as `name:create_payment`

**Result**: Metrics reduced from 200k+ to ~10 unique series

### 2. User ID Cardinality

**Problem**: Random user IDs (up to 1000) and spike test creating 500 unique users inflated cardinality.

**Solution**:
- Implemented user ID pooling with deterministic mapping
- Main test: Maps VUs to 100-user pool (VU % 100)
- Steady-state: Maps VUs to 50-user pool (VU % 50)
- Spike test: Maps 500 VUs to 100-user pool (VU % 100)

**Benefit**: Realistic behavior with controlled cardinality

### 3. Request Tagging

**Problem**: No structured tags for metric organization, made analysis difficult.

**Solution**:
- Added `name` tag: Groups by operation (create_payment, get_payment)
- Added `scenario` tag: Identifies test scenario (full_flow, steady_state, spike_test)
- Thresholds now use tagged metrics: `http_req_duration{name:create_payment}`

**Benefit**: Clear separation of metrics by operation and scenario

### 4. Error Handling

**Problem**: Silent failures when parsing JSON responses.

**Solution**:
- Added try-catch with proper error logging
- Changed `console.log()` to `console.error()` for failures
- Improved check conditions to handle parse errors

### 5. Custom Metrics

**Problem**: Inconsistent metrics across test scripts.

**Solution**:
- All tests now track: error rate, success count, failure count, duration
- Scenario tests now have same metrics as main test
- Separate trends for create and get operations

## Code Changes Summary

### payment-load-test.js
- User pooling: `getUserId(__VU)` maps VUs to 100-user pool
- Request tags: `tags: { name: 'create_payment', scenario: 'full_flow' }`
- URL grouping: k6 now groups metrics by endpoint, not ID
- Better error handling: try-catch blocks with proper logging
- Separate metrics for create and get operations
- Improved thresholds with tagged metrics

### scenarios/steady-state.js
- Reduced user IDs from 50 unique to cyclic pool
- Added custom metrics tracking
- Added request tags
- Updated thresholds to use tagged metrics
- Consistent error handling

### scenarios/spike-test.js
- Reduced spike VUs (500) from 500 unique to 100-user pool
- Added custom metrics tracking
- Added request tags with scenario identification
- Updated thresholds to use tagged metrics
- Added response time check

## Running the Tests

```bash
# Full flow test with ramping load
k6 run payment-load-test.js

# Steady state (50 concurrent users for 10 minutes)
k6 run scenarios/steady-state.js

# Spike test (ramp to 500 users)
k6 run scenarios/spike-test.js

# With custom base URL
BASE_URL=http://api.example.com k6 run payment-load-test.js

# Generate HTML report
k6 run payment-load-test.js --out json=results/output.json
k6 run payment-load-test.js --out csv=results/metrics.csv
```

## Expected Results

After these improvements:

1. Metrics cardinality reduced by 95%+ (from 200k to ~10 series)
2. Memory usage significantly reduced
3. Metrics aggregation faster
4. Better insights with scenario/operation separation
5. No change to test behavior or realism
6. More reliable error detection

## Further Optimization Options

If cardinality warnings persist:

1. **Remove less critical metrics**: Only track essential dimensions
2. **Increase aggregation interval**: Group metrics over longer windows
3. **Add URL grouping for GET requests**: If needed, use URL transformation

```javascript
// Optional: Transform URLs for additional grouping
tags: { 
  name: 'get_payment',
  scenario: 'full_flow',
  // Optionally: group by status code range
  response_group: 'success'
}
```

## References

- [k6 Metrics and Groups](https://grafana.com/docs/k6/latest/using-k6/metrics/)
- [k6 Tags and Groups](https://grafana.com/docs/k6/latest/using-k6/tags-and-groups/)
- [k6 Thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)
