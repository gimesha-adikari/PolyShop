import http from "k6/http";
import { sleep } from "k6";

export let options = {
    stages: [
        { duration: "1m", target: 50 },
        { duration: "2h", target: 50 },
        { duration: "1m", target: 0 },
    ],
};

export default function () {
    http.get("http://localhost:8080/orders");
    sleep(1);
}