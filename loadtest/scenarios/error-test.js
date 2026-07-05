/**
 * Error Test Scenario for Loki & Jaeger Validation
 *
 * Generates various error conditions to validate log aggregation and tracing:
 * - Authentication errors (401 Unauthorized)
 * - Validation errors (400 Bad Request)
 * - Authorization errors (403 Forbidden)
 * - Rate limit errors (429 Too Many Requests)
 * - Not found errors (404 Not Found)
 * - Server errors (5xx if triggerable)
 *
 * Run: k6 run scenarios/error-test.js
 * Check results in:
 *   - Loki: http://localhost:3000/d/loki-logs-detailed
 *   - Jaeger: http://localhost:16686
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOAD_TEST_USERNAME = __ENV.LOAD_TEST_USERNAME || 'load_test_user';
const LOAD_TEST_PASSWORD = __ENV.LOAD_TEST_PASSWORD || 'LoadTest123!';

// Custom metrics for error tracking
const authErrors = new Counter('auth_errors');
const validationErrors = new Counter('validation_errors');
const rateLimitErrors = new Counter('rate_limit_errors');
const notFoundErrors = new Counter('not_found_errors');
const otherErrors = new Counter('other_errors');
const errorRate = new Rate('error_rate');

export const options = {
  vus: 10,
  duration: '3m',
  thresholds: {
    'error_rate': ['rate<1.0'],  // We expect errors - this should always pass
  },
};

export function setup() {
  // Get valid token for subsequent tests
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: LOAD_TEST_USERNAME, password: LOAD_TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) {
    throw new Error(`Login failed: ${loginRes.status} - ${loginRes.body}`);
  }

  const accessToken = JSON.parse(loginRes.body).accessToken;
  console.log('Setup: Got valid access token for error testing');
  return { token: accessToken };
}

export default function (data) {
  // Test 1: Authentication Error (missing token)
  group('Test 1: Missing Authentication Token (401)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        amount: 100.00,
        currency: 'MXN',
        merchant: 'error_test',
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { error_type: 'auth', scenario: 'error_test' },
      }
    );

    const isAuthError = check(res, {
      'status is 401 Unauthorized': (r) => r.status === 401,
    });

    if (isAuthError) {
      authErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isAuthError ? 0 : 1);
  });

  sleep(0.5);

  // Test 2: Invalid Token (401)
  group('Test 2: Invalid Bearer Token (401)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        amount: 100.00,
        currency: 'MXN',
        merchant: 'error_test',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer invalid.token.here',
        },
        tags: { error_type: 'invalid_token', scenario: 'error_test' },
      }
    );

    const isAuthError = check(res, {
      'status is 401 Unauthorized': (r) => r.status === 401,
    });

    if (isAuthError) {
      authErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isAuthError ? 0 : 1);
  });

  sleep(0.5);

  // Test 3: Invalid Payment Data (400 Bad Request)
  group('Test 3: Invalid Payment Data - Missing Amount (400)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        currency: 'MXN',
        merchant: 'error_test',
        // Missing required 'amount' field
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${data.token}`,
        },
        tags: { error_type: 'validation', scenario: 'error_test' },
      }
    );

    const isValidationError = check(res, {
      'status is 400 Bad Request': (r) => r.status === 400,
    });

    if (isValidationError) {
      validationErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isValidationError ? 0 : 1);
  });

  sleep(0.5);

  // Test 4: Negative Amount (400 Bad Request)
  group('Test 4: Invalid Amount - Negative Value (400)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        amount: -50.00,
        currency: 'MXN',
        merchant: 'error_test',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${data.token}`,
        },
        tags: { error_type: 'validation', scenario: 'error_test' },
      }
    );

    const isValidationError = check(res, {
      'status is 400 Bad Request': (r) => r.status === 400,
    });

    if (isValidationError) {
      validationErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isValidationError ? 0 : 1);
  });

  sleep(0.5);

  // Test 5: Invalid Currency (400)
  group('Test 5: Invalid Currency Code (400)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        amount: 50.00,
        currency: 'INVALID',
        merchant: 'error_test',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${data.token}`,
        },
        tags: { error_type: 'validation', scenario: 'error_test' },
      }
    );

    const isValidationError = check(res, {
      'status is 400 Bad Request': (r) => r.status === 400,
    });

    if (isValidationError) {
      validationErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isValidationError ? 0 : 1);
  });

  sleep(0.5);

  // Test 6: Get Non-existent Payment (404)
  group('Test 6: Get Non-existent Payment (404)', () => {
    const fakePaymentId = '00000000-0000-0000-0000-000000000000';
    const res = http.get(
      `${BASE_URL}/api/v1/payments/${fakePaymentId}`,
      {
        headers: {
          'Authorization': `Bearer ${data.token}`,
        },
        tags: { error_type: 'not_found', scenario: 'error_test' },
      }
    );

    const isNotFoundError = check(res, {
      'status is 404 Not Found': (r) => r.status === 404,
    });

    if (isNotFoundError) {
      notFoundErrors.add(1);
    } else {
      otherErrors.add(1);
    }
    errorRate.add(!isNotFoundError ? 0 : 1);
  });

  sleep(0.5);

  // Test 7: Rate Limiting - Rapid Fire Requests (429)
  group('Test 7: Rate Limit - Rapid Login Attempts (429)', () => {
    for (let i = 0; i < 10; i++) {
      const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
          username: 'nonexistent_user',
          password: 'wrong_password',
        }),
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { error_type: 'rate_limit', scenario: 'error_test' },
        }
      );

      // Expect 401 for invalid credentials, but if rate limited -> 429
      const isRateLimited = check(res, {
        'status is 429 Too Many Requests or 401 Unauthorized': (r) =>
          r.status === 429 || r.status === 401,
      });

      if (res.status === 429) {
        rateLimitErrors.add(1);
      } else if (res.status === 401) {
        authErrors.add(1);
      } else {
        otherErrors.add(1);
      }
      errorRate.add(!isRateLimited ? 0 : 1);
    }
  });

  sleep(1);

  // Test 8: Successful Payment (for comparison in logs)
  group('Test 8: Successful Payment (201)', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify({
        amount: 50.00,
        currency: 'MXN',
        merchant: 'error_test_success',
        description: 'Successful payment for error test validation',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${data.token}`,
        },
        tags: { error_type: 'success', scenario: 'error_test' },
      }
    );

    check(res, {
      'status is 201 Created': (r) => r.status === 201,
      'has paymentId': (r) => {
        try {
          return JSON.parse(r.body).paymentId !== undefined;
        } catch {
          return false;
        }
      },
    });

    errorRate.add(0);  // Success, no error
  });

  sleep(1);
}
