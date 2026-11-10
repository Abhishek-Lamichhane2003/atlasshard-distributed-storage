import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 20 },
    { duration: '30s', target: 40 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<250'],
  },
};

export default function () {
  const overview = http.get('http://localhost:8080/api/system/overview');
  check(overview, { 'overview is 200': response => response.status === 200 });
  const nodes = http.get('http://localhost:8080/api/nodes');
  check(nodes, { 'nodes is 200': response => response.status === 200 });
  sleep(0.5);
}
