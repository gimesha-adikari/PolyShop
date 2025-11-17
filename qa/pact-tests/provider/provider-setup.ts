import path from "path";
import { Verifier } from "@pact-foundation/pact";
import dotenv from "dotenv";

dotenv.config();

export async function verifyPacts() {
    const verifier = new Verifier({
        provider: "PolyShopProvider",
        logLevel: "info",
        providerBaseUrl: process.env.PROVIDER_BASE_URL || "http://localhost:8080",
        pactUrls: [
            path.resolve(__dirname, "../pacts/PolyShopConsumer-PolyShopProvider.json")
        ],
        publishVerificationResult: false,
    });

    return verifier.verifyProvider().then(output => {
        console.log("Pact Verification Complete");
        console.log(output);
    });
}
