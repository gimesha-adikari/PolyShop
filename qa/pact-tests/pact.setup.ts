import path from "path";
import { Pact } from "@pact-foundation/pact";

export const provider = new Pact({
    consumer: "PolyShopFrontend",
    provider: "PolyShopAuthService",
    port: 8990,
    log: path.resolve(process.cwd(), "logs", "pact.log"),
    dir: path.resolve(process.cwd(), "pacts"),
    logLevel: "info",
    spec: 2
});
