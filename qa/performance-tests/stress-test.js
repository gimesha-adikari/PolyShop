import http from "k6/http";
import { sleep } from "k6";

export let options = {
    stages: [
        { duration: "1m", target: 200 },
        { duration: "3m", target: 200 },
        { duration: "30s", target: 0 },
    ],
};

export default function () {
    http.get("http://localhost:8080/products");
    sleep(1);
}
