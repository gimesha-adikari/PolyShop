# PolyShop Security Architecture

Version: 1.0.0  
Owner: Platform / Security  
Scope: All PolyShop services (Auth, Product, Inventory, Order, Payment, Notification, Search, Analytics, Gateway)

---

## 1. Goals & Threat Model

### 1.1 Security Goals

- Protect user identities, credentials, and payments.
- Enforce least-privilege access between:
  - Users ↔ API Gateway
  - API Gateway ↔ Backend services
  - Service ↔ Service
- Provide a consistent, centralized authentication & authorization model.
- Support evolution to advanced features (MFA, device trust, risk-based auth) without breaking clients.
- Make debugging and incident response easier through strong observability & audit trails.

### 1.2 High-Level Threats

- Credential theft (password reuse, phishing, database leak).
- Token theft (XSS, local storage compromise, stolen device).
- Broken access control (user accessing another user’s orders, admin endpoints exposed).
- Data tampering in transit (MITM) or at rest (DB compromise).
- Replay & double-submit attacks for critical operations (payments/orders).
- Compromised internal service misusing internal APIs.

---

## 2. Identity & Authentication Overview

### 2.1 Identity Provider

- **Auth Service** is the **single source of truth** for:
  - User accounts (email, password hash, roles, status).
  - Sessions & refresh tokens.
  - Email verification & password reset tokens.
  - Future MFA factors.

All other services (Order, Payment, etc.) treat Auth as an **IDP** and trust only:

- Valid **JWT access tokens** signed by Auth.
- Internal **service tokens** issued/configured by platform.

### 2.2 Authentication Flows (User-Facing)

1. **Register**
   - `POST /auth/register` (Auth Service).
   - Provides email, password, fullName.
   - Password is hashed with strong KDF (see Section 4).
   - Email verification token generated and email sent via Notification Service.
   - Response returns an initial `AuthResponse` (access + refresh) OR a minimal response (configurable).

2. **Login**
   - `POST /auth/login`.
   - Validates email + password.
   - On success:
     - Creates a **Session** row (device, IP, userAgent).
     - Issues short-lived **access token (JWT)**.
     - Issues longer-lived **refresh token (opaque string)**.
     - Returns `AuthResponse` with both tokens and user info.

3. **Refresh**
   - `POST /auth/refresh`.
   - Client sends refresh token.
   - Auth service validates token (via DB/Redis).
   - If valid and not revoked:
     - Issues new access token.
     - Optionally rotates refresh token (configurable: rolling vs. fixed).

4. **Logout**
   - `POST /auth/logout`.
   - If refresh token specified:
     - Mark that token as revoked in DB/Redis.
   - If body is empty:
     - Revoke all active tokens for that user (policy decision).
   - Access token becomes useless after its natural expiry.

5. **Sessions Management**
   - `GET /auth/sessions` to list active sessions for a user.
   - `DELETE /auth/sessions/{sessionId}` to revoke a specific session.
   - Helps handle stolen tokens/devices and multi-device management.

---

## 3. Token Model

### 3.1 Access Token (JWT)

- **Type**: JWT, signed (JWS) with asymmetric keys (preferred):
  - `alg`: `RS256` or `ES256`
  - Issuer: `iss = "polyshop-auth"`
  - Audience:
    - Public APIs: `aud = "polyshop-public"`
    - Internal services: `aud = "polyshop-internal"`

- **Lifetime**:
  - Short: 5–15 minutes (configurable via env `AUTH_ACCESS_TOKEN_TTL_SECONDS`).

- **Location (client)**:
  - For browser apps:
    - Prefer **HTTP-only, secure cookies** for access token to reduce XSS risk.
    - Or store only in memory (no `localStorage`) for SPA.
  - For trusted backend clients / API consumers: Bearer token in `Authorization` header.

- **Claims (minimum set)**:
  - `sub`: user UUID.
  - `iss`, `aud`, `iat`, `exp`, `jti`.
  - `scope` or `roles`: array of role strings (`["USER"]`, `["ADMIN"]`, etc.).
  - `email` (optional convenience claim).
  - `sid`: session id (links JWT to a Session row).
  - `tenant` (reserved for future multi-tenancy).

### 3.2 Refresh Token (Opaque)

- **Type**: Opaque random string (e.g. 256-bit, base64 or hex).
- **Storage**:
  - Stored server-side in DB or Redis with:
    - token hash (not plaintext).
    - userId.
    - sessionId.
    - expiry.
    - status (ACTIVE/REVOKED/EXPIRED).
  - Exposed to client only once at login/refresh.
- **Lifetime**:
  - 7–30 days configurable (`AUTH_REFRESH_TOKEN_TTL_DAYS`).
- **Rotation**:
  - On refresh:
    - Option A (recommended): rotate (issue new token, invalidate old).
    - Option B: fixed per session (simpler, less secure).

### 3.3 Service-to-Service Tokens

- **Pattern**: Two options, both supported.

1. **JWT with `client_credentials` semantics**
   - Each service has a **service identity** with:
     - `clientId`, `clientSecret`.
   - They request internal tokens (or are issued long-lived ones) with:
     - `sub`: service name (e.g. `order-service`).
     - `scope/aud`: other services it can call.

2. **Static Signed Tokens / API Keys**
   - For early phases, a shared secret or API key between services can be used.
   - Stored in environment variables per service.
   - Exchanged using `Authorization: Bearer <service-token>` or `x-api-key`.

Recommended approach: **JWT service tokens** using the same Auth signing keys but different audiences and scopes.

---

## 4. Credential & Secret Security

### 4.1 Password Hashing

- Algorithm: **Argon2id** or **bcrypt** (with strong cost factor).
- Parameters (example):
  - For bcrypt: cost factor `12–14`.
  - For Argon2id: memory/time parameters tuned for server hardware.
- Never store passwords in plaintext.
- Store:
  - `hash`, `algorithm`, `parameters`, `createdAt`.
- Password change:
  - Verify `currentPassword` first.
  - Re-hash using latest recommended parameters.

### 4.2 Other Sensitive Values

Stored hashed/obfuscated:

- Email verification tokens.
- Password reset tokens.
- MFA secrets (TOTP seeds).

For tokens delivered via email/SMS, only **hashed version** is stored in DB. On use, token from client is hashed and compared.

### 4.3 Secret Management

- All secrets must be read from environment or secret store:
  - JWT private keys.
  - Database credentials.
  - Stripe/PayPal keys.
  - SMTP provider credentials.
- For production:
  - Use **Vault** / cloud-specific secret manager (documented in `/security/vault-policies/`).
- Secret rotation:
  - Support multiple signing keys at once via **key IDs** (`kid`).
  - Token verification looks up public key by `kid`.
  - Allows safe roll-over without downtime.

---

## 5. Authorization & RBAC

### 5.1 Role Model

Minimum roles:

- `USER`: Normal customer.
- `ADMIN`: Full admin for platform.
- `OPS` (optional): DevOps / SRE operations.
- Future: `SELLER`, `SUPPORT`, etc.

The **Auth Service** stores:

- User ↔ Roles (many-to-many).
- Role ↔ Permissions (optional extension).

### 5.2 Authorization Strategy

- **Gateway-level**:
  - Public endpoints accessible without token: `register`, `login`, `health`, `request-password-reset`, `confirm-password-reset`.
  - All other routes require `Authorization: Bearer <accessToken>`.
  - Gateway validates token, expiration, signature, issuer, and audience.
  - Gateway injects:
    - `X-User-Id`
    - `X-User-Roles`
    - `X-Request-Id`
  - Backend services trust these headers only if request comes from the gateway network / mutual TLS (mTLS).

- **Service-level**:
  - Services still validate:
    - Token structure and signature (defence in depth).
    - Required roles for specific endpoints.
  - Example:
    - Order Service: `GET /orders` uses `sub` as userId. Admins can query any user (with special endpoints).
    - Auth Service: `/users` endpoints require `ADMIN` role.

### 5.3 Resource-Level Authorization Examples

- **Orders**
  - A `USER` can only access orders where `order.userId == token.sub`.
  - An `ADMIN` can access any order.

- **Product Management**
  - Public: read-only product list and product detail.
  - Authenticated: no special privileges by default.
  - `ADMIN` or `PRODUCT_MANAGER` (future): can create/update/archive products and variants.

- **Analytics**
  - Only `ADMIN` or `OPS` (e.g. `ANALYST`) can access analytics endpoints.

---

## 6. API Gateway Enforcement

### 6.1 Responsibilities

- TLS termination (HTTPS).
- JWT validation for incoming user tokens.
- Forwarding `X-User-*` and `X-Request-Id`.
- Routing to internal services based on path.
- Rate limiting & throttling.
- Request/response logging (sanitised).
- CORS config for web clients.

### 6.2 Validation Rules

- Require `Authorization` header for any protected path.
- Reject:
  - Expired tokens.
  - Tokens with invalid signatures or unknown `kid`.
  - Tokens with wrong `aud` or `iss`.
- Optionally check:
  - `scope`/`roles` for certain path patterns.

### 6.3 Rate Limiting / Abuse Protection (Phase 2+)

- Per-IP and per-user rate limits for:
  - `/auth/login`
  - `/auth/refresh`
  - `/payments`
- 429 responses with generic error codes.

---

## 7. Service-to-Service Security

### 7.1 Internal Network & mTLS

- All services run within a **trusted cluster network** (Docker Compose / Kubernetes namespace).
- For production:
  - Enable **mTLS** between services:
    - Each service has its own certificate issued by an internal CA.
    - Gateway verifies client certificates for internal calls.

### 7.2 Internal Auth

- Services must authenticate when calling another service:
  - Option 1: Use static service token (config via env).
  - Option 2: Use JWT service token (preferred).
- Example:
  - Order Service calls Payment Service:
    - Adds `Authorization: Bearer <order-service-internal-token>`.
    - Payment validates:
      - Token signature.
      - `sub = "order-service"`.
      - `aud = "payment-service"` or `aud = "polyshop-internal"`.

### 7.3 Principle of Least Privilege

- Internal tokens carry **scopes**:
  - `order:read`, `order:write`, `payment:create` etc.
- A service gets only the scopes needed.
- For example:
  - Search Service might only need `product:read` and never `auth:write`.

---

## 8. Token Revocation & Session Model

### 8.1 Session Entity (Conceptual)

For Auth DB:

- `Session` fields:
  - `id`: UUID.
  - `userId`: UUID.
  - `refreshTokenHash`: string.
  - `userAgent`: string (nullable).
  - `ipAddress`: string (nullable).
  - `createdAt`, `lastUsedAt`.
  - `expiresAt`.
  - `revokedAt` / `revokedReason`.
  - `current`: boolean (for UI convenience).

### 8.2 Revocation Logic

- On logout with specific token:
  - Mark `revokedAt` and optionally set `current = false`.
- On logout-all:
  - Mark all sessions for the user as revoked.
- On suspicious activity:
  - Admin / automated process can revoke sessions for user or IP.

### 8.3 Access Tokens and Revocation

- Access tokens are short-lived and **not** stored in DB.
- Revoking a refresh token eventually kills the ability to obtain new tokens.
- For **high-risk** cases (account takeover):
  - Maintain a short-lived **denylist** (e.g. in Redis) for specific `jti` values to immediately reject existing access tokens.

---

## 9. MFA & Future Features (Future-Proofing)

Even if not implemented in Phase 1, the model is designed to support MFA.

### 9.1 MFA Methods

- TOTP-based MFA (e.g. Google Authenticator).
- Email / SMS OTP.
- WebAuthn (security keys) as a long-term goal.

### 9.2 Data Model Extensions

- `UserMfaMethod`:
  - userId
  - type (TOTP, SMS, EMAIL, WEBAUTHN)
  - secret/seed (encrypted)
  - enabled, createdAt, lastUsedAt

- `MfaChallenge`:
  - challengeId
  - userId
  - type
  - issuedAt
  - expiresAt
  - verifiedAt

### 9.3 Auth Flow Impact

- `login` may return:
  - status: `MFA_REQUIRED`.
  - partial session ID.
- Client then calls:
  - `POST /auth/mfa/verify` with code & challengeId.
- On success:
  - Full `AuthResponse` issued.
- Token claims can include `amr` (Authentication Methods Reference) to capture MFA state.

---

## 10. Data Protection & Storage

### 10.1 At Rest

- Databases:
  - Use built-in volume / disk encryption (cloud / OS-level).
  - Sensitive columns (Stripe secrets, TOTP seeds) encrypted with application key or KMS.
- Backups:
  - Encrypted at rest and in transit.
  - Access restricted to ops / backup system.

### 10.2 In Transit

- All public traffic via HTTPS only.
- Internal traffic via:
  - mTLS (production).
  - At minimum, cluster-private network (dev).

---

## 11. Audit Logging & Observability

### 11.1 Audit Events

Auth Service logs:

- Logins (success/failure).
- Password changes.
- Email verification events.
- Password reset requests and confirmations.
- Role changes and admin actions.
- Session creation/revocation.

Order / Payment services log:

- Order creation & status changes.
- Payment intent creation, success, failure, refunds.

### 11.2 Structure

- Use structured logs (JSON) with fields:
  - `timestamp`
  - `service`
  - `userId` (where applicable)
  - `sessionId` (if available)
  - `requestId` (`X-Request-Id`)
  - `action` (e.g. `AUTH.LOGIN.SUCCESS`)
  - `context` (orderId, paymentId, etc.)

### 11.3 Correlation IDs

- Gateway generates a `X-Request-Id` for every incoming request (if not provided).
- Propagated to all downstream services.
- Included in logs and error responses (as `requestId`) for correlation.

---

## 12. Security Testing & Hardening

### 12.1 Automated Checks

- Static analysis & dependency scanning (SCA) in CI:
  - OWASP dependency check / Snyk / GitHub Dependabot.
- Linting & code quality for all services.

### 12.2 Manual & Periodic Checks

- Penetration tests on public Gateway endpoints.
- Regular review of:
  - Auth flows.
  - Token handling on frontend.
  - Storage of secrets.

### 12.3 Secure Defaults

- CORS locked down to known frontends in production.
- Strict HTTP headers at Gateway:
  - `Strict-Transport-Security`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY` or `SAMEORIGIN`
  - `Content-Security-Policy` (tuned for frontends).

---

## 13. Summary

- **Auth Service** is the central identity & token authority.
- **JWT access tokens** are short-lived and include minimal claims (userId, roles, etc.).
- **Refresh tokens** are opaque, stored server-side, and revocable, linked to Sessions.
- **RBAC** is role-based with clear separation of customer vs admin vs ops functionality.
- **Gateway** acts as the main enforcement point for authN + authZ and propagates identity to services.
- **Service-to-service security** uses internal tokens and mTLS to prevent unauthorized internal calls.
- Design is **future-proof** for MFA, advanced RBAC, and multi-tenancy without breaking current clients.
