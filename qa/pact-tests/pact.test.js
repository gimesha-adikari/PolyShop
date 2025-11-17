const path = require("path");
const { Pact } = require("@pact-foundation/pact");
const { like, uuid } = require("@pact-foundation/pact").Matchers;
const axios = require("axios");
const chai = require("chai");
const expect = chai.expect;

describe("PolyShop Consumer Contract - Product Service via Gateway", () => {
    const provider = new Pact({
        consumer: "polyshop-frontend",
        provider: "polyshop-api-gateway",
        port: 9999,
        log: path.resolve(process.cwd(), "logs", "pact.log"),
        dir: path.resolve(process.cwd(), "pacts"),
        logLevel: "info"
    });

    before(() => provider.setup());
    after(() => provider.finalize());
    afterEach(() => provider.verify());

    describe("GET /products", () => {
        before(() =>
            provider.addInteraction({
                state: "products exist",
                uponReceiving: "a request for product list",
                withRequest: {
                    method: "GET",
                    path: "/products",
                    query: "page=1&perPage=10"
                },
                willRespondWith: {
                    status: 200,
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: {
                        items: like([
                            {
                                id: uuid("11111111-1111-1111-1111-111111111111"),
                                name: like("Example Product"),
                                price: like(49.99),
                                currency: like("USD"),
                                status: like("ACTIVE")
                            }
                        ]),
                        page: like(1),
                        perPage: like(10),
                        total: like(100)
                    }
                }
            })
        );

        it("returns list of products", async () => {
            const response = await axios.get("http://localhost:9999/products?page=1&perPage=10");
            expect(response.status).to.equal(200);
            expect(response.data.items).to.be.an("array");
            expect(response.data.items[0]).to.have.property("id");
        });
    });
});
