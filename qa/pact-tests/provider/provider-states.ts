export default {
    "product exists": async () => {
        return {
            product: {
                id: "11111111-1111-1111-1111-111111111111",
                name: "Test Jacket",
                price: 79.99,
                currency: "USD"
            }
        };
    },

    "order exists": async () => {
        return {
            order: {
                id: "22222222-2222-2222-2222-222222222222",
                userId: "33333333-3333-3333-3333-333333333333"
            }
        };
    }
};
