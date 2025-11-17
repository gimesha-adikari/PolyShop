import http from "k6/http";
import { sleep, check } from "k6";

export const options = {
    stages: [
        { duration: "10s", target: 10 },   // ramp-up to 10 VUs
        { duration: "20s", target: 25 },   // moderate load
        { duration: "20s", target: 50 },   // heavy load
        { duration: "10s", target: 0 }     // ramp-down
    ],
    thresholds: {
        http_req_duration: ["p(95)<350"],  // 95% of requests < 350ms
        http_req_failed: ["rate<0.01"],    // fail rate < 1%
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
    const res = http.get(`${BASE_URL}/products?page=1&perPage=10`);

    check(res, {
        "status is 200": (r) => r.status === 200,
        "returns array": (r) => Array.isArray(r.json("items")),
    });

    sleep(1);
}
