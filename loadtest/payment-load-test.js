import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics with low cardinality tags
const errorRate = new Rate('errors');
const createPaymentDuration = new Trend('create_payment_duration_ms');
const getPaymentDuration = new Trend('get_payment_duration_ms');
const successCount = new Counter('payments_success');
const failureCount = new Counter('payments_failed');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = '/api/v1';

// Pool of stable user IDs to reduce cardinality
const USER_POOL_SIZE = 100;
const getUserId = (vu) => `user_${(vu % USER_POOL_SIZE) + 1}`;

export const options = {
  stages: [
    { duration: '1m', target: 10 },    // Ramp-up to 10 users over 1 minute
    { duration: '3m', target: 50 },    // Ramp-up to 50 users
    { duration: '5m', target: 50 },    // Stay at 50 users
    { duration: '2m', target: 100 },   // Ramp-up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '3m', target: 0 },     // Cool-down to 0
  ],
  thresholds: {
    'http_req_duration{name:create_payment}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:get_payment}': ['p(95)<200', 'p(99)<500'],
    'http_req_failed': ['rate<0.1'],
    'errors': ['rate<0.1'],
  },
};

export default function () {
  group('Create Payment', function () {
    const createPaymentPayload = {
      userId: getUserId(__VU),
      amount: Math.floor(Math.random() * 10000) / 100,  // Random 0-100
      currency: 'MXN',
      merchant: `merchant_${Math.floor(Math.random() * 20)}`,
      description: 'Load test payment',
    };

    const createRes = http.post(
      `${BASE_URL}${API_PREFIX}/payments`,
      JSON.stringify(createPaymentPayload),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'create_payment', scenario: 'full_flow' },
      }
    );

    const success = check(createRes, {
      'create payment status is 201': (r) => r.status === 201,
      'create payment has paymentId': (r) => {
        try {
          return JSON.parse(r.body).paymentId !== undefined;
        } catch {
          return false;
        }
      },
      'create payment response time < 500ms': (r) => r.timings.duration < 500,
    });

    createPaymentDuration.add(createRes.timings.duration);

    if (success) {
      successCount.add(1);
      errorRate.add(0);
    } else {
      failureCount.add(1);
      errorRate.add(1);
    }

    // Extract payment ID for next request
    let paymentId;
    try {
      paymentId = JSON.parse(createRes.body).paymentId;
    } catch (e) {
      console.error('Failed to parse payment ID from response');
      return;
    }

    sleep(1);

    // Get Payment - use URL grouping to avoid cardinality explosion
    group('Get Payment', function () {
      const getRes = http.get(
        `${BASE_URL}${API_PREFIX}/payments/${paymentId}`,
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { name: 'get_payment', scenario: 'full_flow' },
        }
      );

      const getSuccess = check(getRes, {
        'get payment status is 200': (r) => r.status === 200,
        'get payment has paymentId': (r) => {
          try {
            return JSON.parse(r.body).paymentId === paymentId;
          } catch {
            return false;
          }
        },
        'get payment response time < 200ms': (r) => r.timings.duration < 200,
      });

      if (!getSuccess) {
        errorRate.add(1);
        failureCount.add(1);
      }

      getPaymentDuration.add(getRes.timings.duration);
    });

    sleep(Math.random() * 3);  // Random sleep 0-3 seconds
  });
}