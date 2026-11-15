import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const MODE = __ENV.MODE || 'catalog'; // catalog | profile | advice | sync

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { target: 20, duration: '5s' },
        { target: 50, duration: '15s' },
        { target: 50, duration: '10s' },
      ],
      gracefulRampDown: '3s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  if (MODE === 'catalog') {
    return {};
  }
  const email = `load-${Date.now()}@example.com`;
  const json = { headers: { 'Content-Type': 'application/json' } };
  const requested = http.post(`${BASE}/v1/auth/request-code`, JSON.stringify({ email }), json);
  const code = requested.json('devCode');
  const verified = http.post(`${BASE}/v1/auth/verify`, JSON.stringify({ email, code }), json);
  const token = verified.json('accessToken');
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  if (MODE === 'sync') {
    return { headers, allergyId: 'aaaaaaaa-1111-4111-8111-111111111111' };
  }
  const medicines = http.get(`${BASE}/v1/catalog/medicines?query=${encodeURIComponent('варфарин')}`, { headers });
  const medicineId = medicines.json()[0].id;
  http.post(`${BASE}/v1/medications`, JSON.stringify({ medicineId, dosage: '5 mg' }), { headers });
  return { token, headers, medicineId };
}

export default function (data) {
  let response;
  if (MODE === 'catalog') {
    response = http.get(`${BASE}/v1/catalog/substances?query=${encodeURIComponent('варфарин')}`);
  } else if (MODE === 'advice') {
    response = http.post(`${BASE}/v1/advice/check`, JSON.stringify({ medicineId: data.medicineId }), { headers: data.headers });
  } else if (MODE === 'sync') {
    const body = {
      allergies: [{
        id: data.allergyId,
        updatedAt: '2099-01-01T00:00:00Z',
        deleted: false,
        name: 'Пенициллин',
        severity: 'HIGH',
      }],
    };
    response = http.post(`${BASE}/v1/sync/push`, JSON.stringify(body), { headers: data.headers });
  } else {
    response = http.get(`${BASE}/v1/profile/allergies`, { headers: data.headers });
  }
  check(response, { 'status is 200': (r) => r.status === 200 });
}
