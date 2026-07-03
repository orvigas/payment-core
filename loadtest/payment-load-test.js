import http from 'k6/http';
import { check, group } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '3m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '3m', target: 0 },
  ],
};

let accessToken;

export function setup() {
  // Get token once before test runs
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ userId: `load_test_user` }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const data = JSON.parse(loginRes.body);
  accessToken = data.accessToken;

  console.log('Setup: Got access token');
  return { token: accessToken };
}

export default function (data) {
  group('Create Payment with Auth', () => {
    const payload = {
      userId: `user_${__VU}`,
      amount: 50.00,
      currency: 'MXN',
      merchant: 'load_test',
    };

    const res = http.post(
      `${BASE_URL}/api/v1/payments`,
      JSON.stringify(payload),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${data.token}`,  // ← Use JWT token
        },
      }
    );

    check(res, {
      'status is 201': (r) => r.status === 201,
      'has paymentId': (r) => JSON.parse(r.body).paymentId !== undefined,
    });
  });
}