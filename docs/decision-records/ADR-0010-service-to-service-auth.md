# ADR-0010: Service-to-Service Authentication Strategy

## Status
Accepted

## Date
2025-11-17

## Context

PolyShop is composed of multiple microservices:
- Auth
- Product
- Inventory
- Order
- Payment
- Notification
- Search
- Analytics
- Gateway

These services must communicate securely with:
- Minimal latency
- Clear access control
- Rotation-friendly credentials
- Environment consistency (local/dev/staging/prod)
- Gateway as public-facing boundary
- Zero direct trust without proof

PolyShop already uses JWT for **end-user authentication**, but internal service-to-service authentication needs a separate and stronger model.

Challenges being solved:
- Prevent public tokens from being used internally
- Limit access between services using least privilege
- Provide identity for machines, not users
- Support future mutual TLS (mTLS) adoption
- Make local development easy without reducing production security

---

# Decision

PolyShop will use **signed internal service JWTs**, minted by the Auth Service, for service-to-service authentication.

A dedicated **Service Token** format will be used:
- Signed by Auth using an internal-only private key
- Valid for short lifetime (5–15 minutes)
- Uses `aud` (audience) field to scope permissions
- Distinct from user tokens
- Revocable via key rotation
- Lightweight and gateway-friendly

All services will validate:
- Signature
- Issuer
- Audience
- Expiration time

Auth flow:

```

Service → Auth Service → Service Token → Target Service

```

---

# Token Format

### Header
```

{
"alg": "RS256",
"typ": "JWT",
"kid": "service-key-2025"
}

```

### Payload
```

{
"iss": "polyshop-auth",
"sub": "product-service",
"aud": ["inventory-service", "order-service"],
"exp": 1737073340,
"iat": 1737073040
}

```

### Constraints
- `sub` identifies the calling service
- `aud` restricts access
- Short-lived tokens only
- Rotated keys every 60–90 days
- Services cache and refresh tokens automatically

---

# Key Distribution Model

Auth Service manages:
- Internal private key (never leaves Auth)
- Rotated public keys (JWKS endpoint)
- Key identifiers (`kid`)

All other services:
- Fetch and cache JWKS keys for signature verification
- Accept only keys from the JWKS list

JWKS Endpoint:
```

GET /.well-known/internal-jwks.json

```

Not exposed via gateway.

---

# Audience Rules

Each PolyShop service has a defined set of audiences it can call.

### Example
Product Service → Inventory Service:
```

aud = ["inventory-service"]

```

Order Service → Payment Service:
```

aud = ["payment-service"]

```

Payment Service → Notification Service:
```

aud = ["notification-service"]

```

Unauthorized calls will be rejected by audience mismatch.

---

# Local Development Strategy

To avoid friction:

### Option A - Shared Static Dev Key (chosen)
- A fixed RSA keypair is baked into local builds
- Added to `.gitignore` for developer overrides
- Perfect for local docker-compose

### Option B - Mock Auth Service issuing tokens
Rejected: too complex for local dev.

---

# Token Refresh Model

Each service uses a simple internal token provider:

```

If token expires in < 1 minute → refresh
Else → reuse cached token

```

Token provider is implemented as part of the PolyShop Common Library.

---

# Service Identity Registry

A small static registry defines:
- Allowed service names
- Allowed audiences for each service

Example:

| Service | Allowed Calls |
|---------|----------------|
| product-service | inventory-service, search-service |
| order-service | payment-service, inventory-service |
| payment-service | notification-service |
| analytics-service | none |
| notification-service | none |

This is a YAML file shipped as config:
```

config/service-permissions.yaml

```

ensuring least-permission communication.

---

# Why Not mTLS Immediately?

mTLS was considered but rejected for current phase due to:
- Complexity in key distribution
- Certificate rotation overhead
- Harder local development

However, this ADR keeps space open to add mTLS later:
- JWT-based identity will still work
- mTLS can be layered without breaking APIs

---

# Alternatives Considered

### Option A — API keys
Rejected: static, insecure, non-rotating.

### Option B — mTLS only
Rejected: operationally heavy.

### Option C — Reuse user JWTs for internal calls
Rejected: violates separation of concerns.

### Option D — No auth between services
Rejected: insecure.

---

# Consequences

### Positive
- Strong separation between user and service identities
- Easy to audit service interactions
- Simple to rotate keys
- Works seamlessly in containers and Kubernetes
- Enables fine-grained permissions via `aud`
- Maintains strong security without heavy infra requirements

### Negative
- Requires periodic refresh and caching logic
- Adds a small extra load to Auth Service for token issuance
- Services must handle JWKS fetching

---

# Final Decision

PolyShop adopts **short-lived internal service JWTs** with strict audience checks as the official mechanism for service-to-service authentication.

This mechanism is mandatory for all microservices starting with version `v1.0.0`.