import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

// 1. Init Stage
export const options = {
    // Run 10,000 total URL creations spread across 25 concurrent Virtual Users
    scenarios: {
        write_contention: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 500,
            maxDuration: '3m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],   // Less than 1% failure rate
        http_req_duration: ['p(95)<2000'], // 95% of writes complete under 2000ms
    },
}

// 2. Setup Stage (Optional)
// Run once
// Using to login for protected routes
export function setup() {
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify(
            { 
                email: 'test@email.com', 
                password: 'password123' 
            }
        ),
        { 
            headers: 
            { 
                'Content-Type': 'application/json' 
            } 
        }
    );

    check(loginRes, { 'login succeeded': (r) => r.status === 200 });

    const jar = http.cookieJar();
    const rawCookies = jar.cookiesForURL(BASE_URL);
    const cookieMap = {};
    Object.keys(rawCookies).forEach((name) => {
        cookieMap[name] = rawCookies[name][0];
    });

    return { cookies: cookieMap };
}

// 3. VU Stage
// Runs 10,000 times across 25 concurrent VUs
export default function (data) {

    const uniqueSuffix = `${__VU}-${__ITER}-${Date.now()}`;
    const payload = JSON.stringify({
        originalUrl: `https://example.com/benchmark/page/${uniqueSuffix}`,
    });

    const res = http.post(`${BASE_URL}/api/v1/url`, payload, {
        headers: { 'Content-Type': 'application/json' },
        cookies: data.cookies,
        tags: { name: 'CreateShortUrl' },
    });

    if (res.status !== 200 && res.status !== 201) {
        console.log(`ITER ${__ITER} FAILED | Status: ${res.status} | Body: ${res.body}`);
    }

    check(res, {
        'created (200/201)': (r) => r.status === 200 || r.status === 201,
    });

}
