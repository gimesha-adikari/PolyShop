# PolyShop Order Saga Workflow
Version: 1.0.0  
Status: Finalized

---

# 1. Purpose

This document defines the complete lifecycle, orchestration logic, compensation behaviors, timeout rules, retry policies, idempotency guarantees, and state machine for the **Order Saga** in the PolyShop microservices system.

The Order Saga ensures that an order moves reliably through the following distributed components:

- **Order Service**
- **Inventory Service**
- **Payment Service**
- **Notification Service**
- **Search & Analytics (async sinks)**

---

# 2. High‑Level Saga Flow

```
Create Order → Reserve Stock → Create Payment → Await Payment Completion
           ↓fail             ↓fail                 ↓fail/timeout
         Cancel Order ← Release Stock ← Mark Payment Failed
```

---

# 3. Services Involved

| Service | Responsibility |
|--------|----------------|
| Order Service | Saga coordinator, state machine owner |
| Inventory Service | Stock reservation, release, adjustments |
| Payment Service | Payment intents, provider redirects, success/fail events |
| Notification Service | Order confirmation emails/SMS |
| Search | Async index update |
| Analytics | Event ingestion |

---

# 4. Saga States

The order transitions through these states:

| State | Meaning |
|-------|---------|
| **CREATED** | Order created with items, initial state |
| **RESERVING_STOCK** | Saga attempting stock reservation |
| **PENDING_PAYMENT** | Stock reserved; waiting for payment |
| **PAYMENT_FAILED** | Payment failed; stock may be released |
| **PAID** | Payment succeeded |
| **FULFILLING** | Fulfillment/packing started |
| **FULFILLED** | Successfully shipped/delivered |
| **CANCELLED** | Cancelled manually or by saga |
| **EXPIRED** | Auto-cancelled after timeout |

---

# 5. Full Saga Happy Path

1. **User places an order**  
   Order Service creates a new order → state = `CREATED`.

2. **Order Service requests stock reservation**  
   Sends event: `inventory.reserve.requested`.

3. **Inventory Service reserves stock**  
   Emits: `inventory.stock.reserved`.

4. **Order Service receives reservation success**  
   Moves state → `PENDING_PAYMENT`.

5. **Order Service requests payment intent**  
   Sends event: `payment.intent.requested`.

6. **Payment provider redirects user**  
   Payment Service emits: `payment.intent.created`.

7. **User completes payment**  
   Provider (Stripe/PayPal) notifies Payment Service.

8. **Payment Service emits `payment.succeeded`**  
   Order moves state → `PAID`.

9. **Order Service triggers fulfillment**  
   State → `FULFILLING`.

10. **Fulfillment completes**  
    State → `FULFILLED`.

11. **Notification Service sends confirmation**.

---

# 6. Detailed Saga Step‑by‑Step

## Step 1 — Order Created

Event emitted:

```json
{
  "type": "order.created",
  "orderId": "uuid",
  "userId": "uuid",
  "items": [...],
  "timestamp": "2025-01-01T10:00:00Z"
}
```

Saga moves to: **RESERVING_STOCK**

---

## Step 2 — Reserve Stock

Order Service calls:

```
POST /reservations
{
  "orderId": "...",
  "productId": "...",
  "variantId": "...",
  "quantity": 2
}
```

Idempotency key:  
`Idempotency-Key: order-{orderId}`

Possible responses:

- `201` → stock reserved → continue
- `409` → insufficient stock → saga aborts

---

## Step 3 — Reservation Success → PENDING_PAYMENT

Event:

```
inventory.stock.reserved
```

Saga moves to: **PENDING_PAYMENT**

---

## Step 4 — Create Payment Intent

Order Service calls Payment Service:

```
POST /payments
{
  "orderId": "...",
  "provider": "STRIPE"
}
```

Payment Service returns:

```
201 Created
{
  "paymentId": "...",
  "checkoutUrl": "https://stripe.com/..."
}
```

Saga waits for provider callback.

---

## Step 5 — Payment Completion

Payment provider notifies Payment Service → Payment Service emits:

```
payment.succeeded
```

Saga moves state → **PAID**

---

## Step 6 — Fulfillment Triggered

Event:

```
order.fulfillment.requested
```

Warehouse or manual process marks:

```
order.fulfillment.completed
```

State → **FULFILLED**

---

# 7. Compensation Logic

## Case A — Inventory Reservation Failure
Trigger:
```
inventory.stock.reservation_failed
```

Actions:

- Mark order state = `CANCELLED`
- Emit `order.cancelled`
- No payment created

## Case B — Payment Failed
Trigger:
```
payment.failed
```

Actions:

- Mark order state = `PAYMENT_FAILED`
- Request inventory release:
  ```
  reservations/{id}/release
  ```
- Mark order state = `CANCELLED`
- Emit notification

## Case C — Payment Timeout

If payment not completed within **15 minutes**:

- Payment Service marks the payment as `EXPIRED`
- Order Service:
  - Releases reservation
  - Marks order = `EXPIRED`

---

# 8. Timeout & Retry Policies

| Action | Timeout | Retries | Behavior |
|--------|---------|---------|----------|
| Reserve stock | 3s | 3 | Fail → Cancel order |
| Payment intent creation | 5s | 3 | Retry with same idempotency key |
| Await payment | 15 min | N/A | Timeout → Cancel order |
| Fulfillment | 24h | manual retry | Ops handled |

---

# 9. Idempotency Guarantees

### Order Service
- All saga steps stored in `order_saga_log`
- No duplicate step execution

### Inventory
- Reservation: idempotent per (orderId + item)
- Release: idempotent

### Payment
- Payment intent: idempotent per orderId
- Webhook receipt: deduplicated by eventId

---

# 10. Saga State Machine Table

| Event | Current State | Next State | Compensation |
|-------|---------------|------------|--------------|
| order.created | NONE | CREATED | — |
| stock.reserved | RESERVING_STOCK | PENDING_PAYMENT | — |
| stock.failed | RESERVING_STOCK | CANCELLED | — |
| payment.intent.created | PENDING_PAYMENT | PENDING_PAYMENT | — |
| payment.succeeded | PENDING_PAYMENT | PAID | — |
| payment.failed | PENDING_PAYMENT | PAYMENT_FAILED | Release stock |
| timeout.payment | PENDING_PAYMENT | EXPIRED | Release stock |
| cancellation.requested | ANY | CANCELLED | If reserved → release stock |

---

# 11. Full Saga Sequence Diagram (PlantUML)

```text
@startuml
actor User
participant OrderService
participant Inventory
participant Payment
participant Notification

User -> OrderService: POST /orders
OrderService -> Inventory: Reserve Stock
Inventory --> OrderService: Stock Reserved

OrderService -> Payment: Create Payment
Payment --> User: Redirect to checkout

User -> Payment: Complete payment
Payment -> OrderService: payment.succeeded

OrderService -> Notification: send order confirmation
@enduml
```

---

# 12. Database Structures Important for Saga

## order table

| column | type | desc |
|--------|------|-------|
| id | uuid | PK |
| user_id | uuid | customer |
| status | enum | order state |
| created_at | timestamp | — |
| updated_at | timestamp | — |

## order_item table

| column | type | desc |
|--------|------|-------|
| order_id | uuid | FK |
| product_id | uuid | — |
| variant_id | uuid | — |
| quantity | int | — |
| unit_price | decimal | — |

## order_saga_log

| id | order_id | step | status | timestamp | data |
|----|----------|------|--------|-----------|-------|

Used for idempotency + recovery.

---

# 13. Recovery Logic (Crash Safety)

If Order Service crashes:

1. On restart, it scans `order_saga_log` for incomplete steps.
2. For each entry:
   - If waiting for reservation → re-try reserve with same idempotency key.
   - If waiting for payment callback → do nothing.
   - If payment expired → cancel.
3. Saga is **fully restart-safe**.

---

# 14. Notifications Triggered

Events:

- `email.order.confirmation`
- `email.payment.failed`
- `email.order.cancelled`
- `email.order.shipped`

Notification Service produces templates.

---

# 15. Final Output Events

The saga emits:

- `order.created`
- `order.reserved`
- `order.pending_payment`
- `order.paid`
- `order.cancelled`
- `order.fulfilled`

Used by Analytics, Search, and UI.

---

# 16. Conclusion

This is the complete, production‑grade Order Saga specification for PolyShop.  
It contains **all behaviors, paths, failure modes, retries, compensation steps, events, and state transitions**.

This file is final and ready for implementation.

