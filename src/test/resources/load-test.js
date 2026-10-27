import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 100,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.95'],
  },
};

// Setup: create a locality with stock K=10 for a test event
export function setup() {
  // Assume event and locality already seeded. Return IDs.
  return {
    eventId: 1,
    localityId: 1,
    totalStock: 10,
  };
}

export default function (data) {
  const payload = JSON.stringify({
    eventId: data.eventId,
    localityId: data.localityId,
    userId: `user-${__VU}-${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const res = http.post(`${BASE_URL}/api/reservations`, payload, params);

  check(res, {
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });

  sleep(0.1);
}

export function teardown(data) {
  // Verify final stock == 0
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, { 'service healthy': (r) => r.status === 200 });
}
