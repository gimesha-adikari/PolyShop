# ADR-0016: Idempotency Strategy

## Status
Accepted

## Date
2025-02-20

## Context
Several PolyShop operations must be idempotent:

- Order creation  
- Payment creation  
- Stock reservation  
- Refund creation  
- Notification sending  

Duplicate requests may occur due to:
- Network retries  
- Kafka replays  
- Client double-submits  
- Gateway retries  

A universal idempotency strategy is required.

## Problem
Without idempotency:
- Orders could be duplicated  
- Payments could charge multiple times  
- Multiple stock reservations could lock too much inventory  

## Requirements
- Idempotency must support:
  - Safe retries  
  - At-least-once messaging  
  - Horizontal scaling  
- Idempotency keys must be:
  - Stable  
  - Traceable  
  - Expirable  
  - Unique per operation  

## Decision
PolyShop will use a **cross-service idempotency strategy**:

### 1. HTTP Services
Client sends:
```

Idempotency-Key: <UUID>

```

Backend stores an **idempotency record** in PostgreSQL:
- key  
- request hash  
- response JSON  
- status  
- expiry timestamp  

If the same key is used again:
- If hash matches → return cached response  
- If hash differs → return 409 Conflict  

### 2. Kafka Consumers
Kafka consumers use:
- **Message-Key based idempotency**, OR
- **Event ID from payload**

Each consumer maintains an **idempotent event table**:
- eventId  
- processedAt  

Events are ignored if already processed.

### 3. Payments
Payment Service uses:
- Stripe/PayPal idempotency keys  
- Our internal idempotency keys  
- Both must align

### 4. Stock Reservation
Inventory Service uses:
`reservationId` as the deterministic idempotency key for:
- reserve  
- confirm  
- release  

### 5. Expiration
Idempotency records auto-expire after:
- 24h for normal operations  
- 7 days for payment operations  

## Consequences
### Pros
- No accidental duplicate orders or charges  
- Compatible with distributed retries  
- Works consistently across all services  

### Cons
- Requires 4–7MB/day extra DB storage  
- Must run periodic cleanup jobs  

## Future Work
- Move idempotency storage into Redis for speed  
- Add API to inspect idempotency keys  
- Gateway could auto-generate keys for unsafe operations  