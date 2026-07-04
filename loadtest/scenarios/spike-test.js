import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Must match a row already seeded via loadtest/seed-load-test-user.sql - login
// now checks real credentials, so there's no implicit user creation anymore.
const LOAD_TEST_USERNAME = __ENV.LOAD_TEST_USERNAME || 'load_test_user';
const LOAD_TEST_PASSWORD = __ENV.LOAD_TEST_PASSWORD || 'LoadTest123!';

// Custom metrics
const errorRate = new Rate('errors');
const paymentDuration = new Trend('create_payment_duration_ms');
const successCount = new Counter('payments_success');
const failureCount = new Counter('payments_failed');

export const options = {
  stages: [
    { duration: '2m', target: 50 },    // Normal load
    { duration: '1m', target: 500 },   // Spike to 500
    { duration: '2m', target: 500 },   // Maintain spike
    { duration: '1m', target: 0 },     // Cool down
  ],
  thresholds: {
    'http_req_duration{name:create_payment}': ['p(99)<2000'],  // Allow higher latency during spike
    'http_req_failed': ['rate<0.2'],      // Allow higher error rate during spike
    'errors': ['rate<0.2'],
  },
};

export function setup() {
  // Get token once before the test runs
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: LOAD_TEST_USERNAME, password: LOAD_TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) {
    throw new Error(
      `Login failed with status ${loginRes.status}: ${loginRes.body}. ` +
      'Run loadtest/seed-load-test-user.sql against the target database first.'
    );
  }

  const accessToken = JSON.parse(loginRes.body).accessToken;
  console.log('Setup: Got access token');
  return { token: accessToken };
}

export default function (data) {
  // The payment owner is derived from the JWT (load_test_user), not a request field.
  const payload = {
    amount: 25.00,
    currency: 'MXN',
    merchant: 'spike_merchant',
    description: 'Spike test payment',
  };

  const res = http.post(
    `${BASE_URL}/api/v1/payments`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`,
      },
      tags: { name: 'create_payment', scenario: 'spike_test' },
    }
  );

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  paymentDuration.add(res.timings.duration);

  if (success) {
    successCount.add(1);
    errorRate.add(0);
  } else {
    failureCount.add(1);
    errorRate.add(1);
  }

  sleep(1);
}