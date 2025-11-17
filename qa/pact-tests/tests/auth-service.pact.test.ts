
import { PactV3, MatchersV3 } from '@pact-foundation/pact';

describe('Auth Service Pact', () => {
  const provider = new PactV3({
    consumer: 'PolyShop-Gateway',
    provider: 'Auth-Service',
  });

  test('login contract', async () => {
    provider.addInteraction({
      states: [{ description: 'User exists' }],
      uponReceiving: 'a login request',
      withRequest: {
        method: 'POST',
        path: '/auth/login',
        body: { email: 'test@example.com', password: '12345678' }
      },
      willRespondWith: {
        status: 200,
        body: {
          accessToken: MatchersV3.string(),
          refreshToken: MatchersV3.string()
        }
      }
    });

    await provider.executeTest(async mock => {
      const res = await fetch(`${mock.uri}/auth/login`, {
        method: 'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({ email:'test@example.com', password:'12345678'})
      });
      expect(res.status).toBe(200);
    });
  });
});
