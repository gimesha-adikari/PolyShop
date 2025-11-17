import { Matchers } from "@pact-foundation/pact";
import { provider } from "./pact.setup";
import axios from "axios";

describe("OrderService Pact Tests", () => {
    beforeAll(() => provider.setup());
    afterAll(() => provider.finalize());

    describe("create order", () => {
        test("successfully creates an order", async () => {
            await provider.addInteraction({
                state: "user is authenticated",
                uponReceiving: "a valid order creation request",
                withRequest: {
                    method: "POST",
                    path: "/orders",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: "Bearer valid-token"
                    },
                    body: {
                        items: [
                            {
                                productId: "11111111-1111-1111-1111-111111111111",
                                quantity: 2
                            }
                        ],
                        shippingAddress: {
                            line1: "123 Street",
                            city: "City",
                            country: "Country"
                        }
                    }
                },
                willRespondWith: {
                    status: 201,
                    headers: { "Content-Type": "application/json" },
                    body: {
                        id: Matchers.uuid(),
                        status: Matchers.like("CREATED"),
                        totalAmount: Matchers.like(150.0),
                        currency: Matchers.like("USD"),
                        userId: Matchers.uuid(),
                        createdAt: Matchers.iso8601DateTime(),
                        items: Matchers.eachLike({
                            productId: Matchers.uuid(),
                            quantity: Matchers.integer(2),
                            unitPrice: Matchers.like(75.0),
                            totalPrice: Matchers.like(150.0)
                        })
                    }
                }
            });

            const response = await axios.post(
                "http://localhost:8991/orders",
                {
                    items: [
                        {
                            productId: "11111111-1111-1111-1111-111111111111",
                            quantity: 2
                        }
                    ],
                    shippingAddress: {
                        line1: "123 Street",
                        city: "City",
                        country: "Country"
                    }
                },
                { headers: { Authorization: "Bearer valid-token" } }
            );

            expect(response.status).toBe(201);
            expect(response.data.id).toBeDefined();
        });
    });
});
