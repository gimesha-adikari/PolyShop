import { verifyPacts } from "./provider-setup";
import providerStates from "./provider-states";

describe("Pact Provider Verification", () => {
    beforeAll(async () => {
        for (const [name, handler] of Object.entries(providerStates)) {
            console.log(`Applying provider state: ${name}`);
            await handler();
        }
    });

    it("verifies all consumer contracts", async () => {
        await verifyPacts();
    });
});
