import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Each VU logs in as its own seeded account (wave_test_user_<VU id>), not a
// single shared login like the other scenarios - see seed-wave-test-users.sql,
// which must be run first and seeds exactly 100 rows to match the peak VU
// count below.
const WAVE_TEST_PASSWORD = __ENV.WAVE_TEST_PASSWORD || 'LoadTest123!';

// Payments per login before a VU "logs out" and re-authenticates, simulating
// a session boundary rather than one login for the VU's entire lifetime.
const ITERATIONS_PER_SESSION = 5;

// Custom metrics
const errorRate = new Rate('errors');
const paymentDuration = new Trend('create_payment_duration_ms');
const paymentsSuccess = new Counter('payments_success');
const paymentsFailed = new Counter('payments_failed');
const loginsSuccess = new Counter('logins_success');
const loginsRateLimited = new Counter('logins_rate_limited');
const loginsFailed = new Counter('logins_failed');
const logouts = new Counter('logouts');

export const options = {
  // Baseline of 20 concurrent users with three spikes up to 100 (the actual
  // "100 users in parallel" peak), each held briefly then brought back down,
  // over a 10-minute run.
  stages: [
    { duration: '30s', target: 20 },  // ramp up to baseline
    { duration: '90s', target: 20 },  // baseline
    { duration: '20s', target: 100 }, // spike 1 ramp
    { duration: '30s', target: 100 }, // spike 1 hold
    { duration: '20s', target: 20 },  // spike 1 recover
    { duration: '90s', target: 20 },  // baseline
    { duration: '20s', target: 100 }, // spike 2 ramp
    { duration: '30s', target: 100 }, // spike 2 hold
    { duration: '20s', target: 20 },  // spike 2 recover
    { duration: '90s', target: 20 },  // baseline
    { duration: '20s', target: 100 }, // spike 3 ramp
    { duration: '30s', target: 100 }, // spike 3 hold
    { duration: '110s', target: 0 },  // ramp down
  ],
  thresholds: {
    // Deliberately no threshold on login success rate or on the overall
    // http_req_failed metric. The login endpoint is rate-limited to 10/min
    // for the whole app instance (RateLimitingConfig), not per user, so with
    // up to 100 individual logins clustering at each ramp-up and spike, most
    // are expected to come back 429. That is real, current production
    // behavior (see docs/SECURITY.md), not a bug in this test - a strict
    // threshold here would just make every run report as "failed" for a
    // known, accepted reason.
    'http_req_duration{name:create_payment}': ['p(95)<1000', 'p(99)<2000'],
  },
};

// Module-scope state is per-VU in k6 (each VU runs its own copy of the
// script), so these naturally act as this VU's session state without
// leaking into other VUs.
let token = null;
let iterationsSinceLogin = 0;

function login() {
  const username = `wave_test_user_${__VU}`;

  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username, password: WAVE_TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );

  if (res.status === 200) {
    token = JSON.parse(res.body).accessToken;
    loginsSuccess.add(1);
    return true;
  }

  if (res.status === 429) {
    loginsRateLimited.add(1);
  } else {
    loginsFailed.add(1);
  }
  token = null;
  return false;
}

// There is no logout endpoint - JWTs here are stateless with no server-side
// revocation (see docs/SECURITY.md) - so logout is simulated the way a real
// stateless-JWT client would do it: just discard the token locally.
function logout() {
  token = null;
  iterationsSinceLogin = 0;
  logouts.add(1);
}

export default function () {
  if (!token) {
    if (!login()) {
      sleep(1);
      return;
    }
  }

  const payload = {
    amount: 15.00,
    currency: 'USD',
    merchant: 'wave_test',
  };

  const res = http.post(
    `${BASE_URL}/api/v1/payments`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      tags: { name: 'create_payment' },
    }
  );

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'has paymentId': (r) => r.status === 201 && JSON.parse(r.body).paymentId !== undefined,
  });

  paymentDuration.add(res.timings.duration);

  if (success) {
    paymentsSuccess.add(1);
    errorRate.add(0);
  } else {
    paymentsFailed.add(1);
    errorRate.add(1);
    // A 401 means the cached token is no longer valid (e.g. expired) - drop
    // it so the next iteration logs back in instead of repeating the failure.
    if (res.status === 401) {
      token = null;
    }
  }

  iterationsSinceLogin++;
  if (iterationsSinceLogin >= ITERATIONS_PER_SESSION) {
    logout();
  }

  sleep(2);
}
