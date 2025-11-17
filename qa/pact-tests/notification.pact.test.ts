import { Matchers } from "@pact-foundation/pact";
import { provider } from "./pact.setup";
import axios from "axios";

describe("NotificationService Pact Tests", () => {
    beforeAll(() => provider.setup());
    afterAll(() => provider.finalize());

    describe("send email notification", () => {
        test("sends email successfully", async () => {
            await provider.addInteraction({
                state: "template exists for order-confirmation",
                uponReceiving: "a request to send an email",
                withRequest: {
                    method: "POST",
                    path: "/notifications/email",
                    headers: { "Content-Type": "application/json" },
                    body: {
                        to: "customer@example.com",
                        templateName: "order-confirmation",
                        variables: {
                            customerName: "John Doe",
                            orderId: "11111111-1111-1111-1111-111111111111"
                        }
                    }
                },
                willRespondWith: {
                    status: 202,
                    headers: { "Content-Type": "application/json" },
                    body: Matchers.like({})
                }
            });

            const response = await axios.post("http://localhost:8995/notifications/email", {
                to: "customer@example.com",
                templateName: "order-confirmation",
                variables: {
                    customerName: "John Doe",
                    orderId: "11111111-1111-1111-1111-111111111111"
                }
            });

            expect(response.status).toBe(202);
        });
    });
});
