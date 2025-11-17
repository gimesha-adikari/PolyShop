import { Matchers } from "@pact-foundation/pact";
import { provider } from "./pact.setup";
import axios from "axios";

describe("AuthService Pact Tests", () => {
    beforeAll(() => provider.setup());
    afterAll(() => provider.finalize());

    describe("login endpoint", () => {
        test("returns tokens + user", async () => {
            await provider.addInteraction({
                state: "user exists",
                uponReceiving: "a valid login request",
                withRequest: {
                    method: "POST",
                    path: "/auth/login",
                    headers: { "Content-Type": "application/json" },
                    body: {
                        email: "test@example.com",
                        password: "password123"
                    }
                },
                willRespondWith: {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                    body: {
                        accessToken: Matchers.like("aaa.bbb.ccc"),
                        refreshToken: Matchers.like("refresh-123"),
                        expiresIn: Matchers.integer(3600),
                        tokenType: "Bearer",
                        user: {
                            id: Matchers.like("11111111-1111-1111-1111-111111111111"),
                            email: "test@example.com",
                            roles: Matchers.eachLike("USER"),
                            enabled: true,
                            emailVerified: true,
                            createdAt: Matchers.like("2025-01-01T00:00:00Z")
                        }
                    }
                }
            });

            const res = await axios.post("http://localhost:8990/auth/login", {
                email: "test@example.com",
                password: "password123"
            });

            expect(res.status).toBe(200);
            expect(res.data.accessToken).toBeDefined();
        });
    });
});
