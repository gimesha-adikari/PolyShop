import { Matchers } from "@pact-foundation/pact";
import { provider } from "./pact.setup";
import axios from "axios";

describe("SearchService Pact Tests", () => {
    beforeAll(() => provider.setup());
    afterAll(() => provider.finalize());

    describe("search products", () => {
        test("returns matching products", async () => {
            await provider.addInteraction({
                state: "products exist in search index",
                uponReceiving: "a search query",
                withRequest: {
                    method: "GET",
                    path: "/search/products",
                    query: { q: "jacket" }
                },
                willRespondWith: {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                    body: {
                        page: Matchers.like(0),
                        size: Matchers.like(20),
                        totalElements: Matchers.like(1),
                        totalPages: Matchers.like(1),
                        content: Matchers.eachLike({
                            productId: Matchers.uuid(),
                            name: Matchers.like("Men's Lightweight Jacket"),
                            description: Matchers.like("Breathable, water-resistant jacket"),
                            price: Matchers.like(79.99),
                            currency: Matchers.like("USD"),
                            score: Matchers.like(1.23)
                        })
                    }
                }
            });

            const response = await axios.get(
                "http://localhost:8994/search/products?q=jacket"
            );

            expect(response.status).toBe(200);
            expect(response.data.content.length).toBeGreaterThan(0);
        });
    });
});
