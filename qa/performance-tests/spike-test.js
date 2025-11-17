import http from "k6/http";
import { sleep } from "k6";

export let options = {
    stages: [
        { duration: "10s", target: 500 },
        { duration: "30s", target: 50 },
        { duration: "10s", target: 0 },
    ],
};

export default function () {
    http.get("http://localhost:8080/products");
    sleep(0.5);
}
