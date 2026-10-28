import http from 'k6/http';
import { check, fail } from 'k6';

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 10,
  duration: '60s',
  thresholds: {
    'http_req_duration{endpoint:reviews_list}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  if (!__ENV.K6_EMAIL || !__ENV.K6_PASSWORD) {
    fail('Set K6_EMAIL and K6_PASSWORD to a test account before running this scenario.');
  }

  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({ email: __ENV.K6_EMAIL, password: __ENV.K6_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  if (response.status !== 200) {
    fail(`Could not authenticate the load-test account (HTTP ${response.status}).`);
  }

  return { token: JSON.parse(response.body).token };
}

export default function ({ token }) {
  const response = http.get(
    `${baseUrl}/api/reviews?page=0&size=10&sort=createdAt,desc`,
    {
      headers: { Authorization: `Bearer ${token}` },
      tags: { endpoint: 'reviews_list' },
    },
  );

  check(response, {
    'review list returns HTTP 200': (result) => result.status === 200,
    'review list has a paginated response': (result) => {
      if (result.status !== 200) return false;
      const page = JSON.parse(result.body);
      return Array.isArray(page.content)
        && typeof page.totalElements === 'number'
        && typeof page.totalPages === 'number';
    },
  });
}
