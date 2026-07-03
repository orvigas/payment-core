import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const errorRate = new Rate('errors');
const paymentDuration = new Trend('create_payment_duration_ms');
const successCount = new Counter('payments_success');
const failureCount = new Counter('payments_failed');

// Pool of stable user IDs to limit cardinality
const USER_POOL_SIZE = 50;
const getUserId = (vu) => `steady_user_${(vu % USER_POOL_SIZE) + 1}`;

export const options = {
  vus: 50,                    // 50 concurrent users
  duration: '10m',            // Run for 10 minutes
  thresholds: {
    'http_req_duration{name:create_payment}': ['p(95)<500', 'p(99)<1000'],
    'http_req_failed': ['rate<0.05'],
    'errors': ['rate<0.05'],
  },
};

export default function () {
  const payload = {
    userId: getUserId(__VU),
    amount: 50.00,
    currency: 'MXN',
    merchant: 'steady_merchant',
    description: 'Steady state test',
  };

  const res = http.post(
    `${BASE_URL}/api/v1/payments`,
    JSON.stringify(payload),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'create_payment', scenario: 'steady_state' },
    }
  );

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  paymentDuration.add(res.timings.duration);

  if (success) {
    successCount.add(1);
    errorRate.add(0);
  } else {
    failureCount.add(1);
    errorRate.add(1);
  }

  sleep(2);  // Simulate realistic user think time
}