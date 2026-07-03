import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const errorRate = new Rate('errors');
const paymentDuration = new Trend('create_payment_duration_ms');
const successCount = new Counter('payments_success');
const failureCount = new Counter('payments_failed');

// Pool to limit cardinality during spike: 500 VUs map to 100 unique users
const USER_POOL_SIZE = 100;
const getUserId = (vu) => `spike_user_${(vu % USER_POOL_SIZE) + 1}`;

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

export default function () {
  const payload = {
    userId: getUserId(__VU),
    amount: 25.00,
    currency: 'MXN',
    merchant: 'spike_merchant',
    description: 'Spike test payment',
  };

  const res = http.post(
    `${BASE_URL}/api/v1/payments`,
    JSON.stringify(payload),
    {
      headers: { 'Content-Type': 'application/json' },
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