import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 1. Init Stage
// Loading the data into shared memory
const shortCodes = new SharedArray('codes', function () {
    const parsed = JSON.parse(open('./shortcodes.json'));
    return parsed[0].json_agg;
})

export const options = {
    discardResponseBodies: true,    // So it won't store buffering response payloads in ram
    stages: [
        { duration: '15s', target: 15 },  // Warm up JVM & connection pools
        { duration: '45s', target: 25 }, // Steady load measurement window
        { duration: '10s', target: 0 },   // Graceful ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95% of redirects must complete under 50ms
        http_req_failed: ['rate<0.01'],    // Less than 1% failed http requests
        checks: ['rate>0.99'],            // >99% of requests must return expected status
    },
};

// 2. VU Stage
// Run in a continuous loop per virtual user
export default function () {
  const code = shortCodes[Math.floor(Math.random() * shortCodes.length)];

  const res = http.get(`http://localhost:8080/${code}`, {
    redirects: 0, 
    tags: { name: 'RedirectEndpoint' },
  });

  check(res, {
    'status is 302': (r) => r.status === 302,
  });

  sleep(0.1); // 100ms pause between requests per VU
}
