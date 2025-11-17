# PolyShop – Communication and Integration Design

## 1. Client → Gateway → Services

- All external traffic goes through **gateway**.
- Clients never talk directly to backend services.
- Gateway responsibilities:
  - Validate JWT from auth-service.
  - Add user context headers if valid.
  - Route to appropriate service based on path.

Example routes:

- `/auth/**` → auth-service
- `/products/**` → product-service
- `/inventory/**` → inventory-service
- `/orders/**` → order-service
- `/payments/**` → payment-service
- `/notify/**` → notification-service
- `/analytics/**` → analytics-service

---

## 2. Synchronous REST Calls Between Services

Used where immediate response is required.

- order-service → product-service
  - Validate product existence and price at checkout.
- order-service → inventory-service
  - Reserve/release stock.
- order-service → payment-service
  - Initiate payment.
- gateway → auth-service
  - Token validation endpoint (if needed for introspection).

These calls should be:
- Idempotent where possible (especially reservations and payment calls).
- Protected with circuit breakers and retries where appropriate.

---

## 3. Asynchronous Event-Driven Communication (Kafka)

Kafka topics:

- `product.events`
  - Events:
    - `product.created`
    - `product.updated`
    - `product.deleted`
  - Producers:
    - product-service
  - Consumers:
    - search-service
    - analytics-service

- `order.events`
  - Events:
    - `order.created`
    - `order.paid`
    - `order.cancelled`
  - Producers:
    - order-service
  - Consumers:
    - notification-service
    - analytics-service

- `payment.events`
  - Events:
    - `payment.initiated`
    - `payment.success`
    - `payment.failed`
  - Producers:
    - payment-service
  - Consumers:
    - order-service
    - notification-service
    - analytics-service

- `notification.events` (optional)
  - Events:
    - `notification.sent`
    - `notification.failed`
  - Producers:
    - notification-service
  - Consumers:
    - analytics-service

---

## 4. Saga / Workflow (Order Lifecycle)

High-level flow:

1. Client calls `POST /orders` (gateway → order-service).
2. order-service:
   - Calls product-service to validate items.
   - Calls inventory-service to reserve stock.
   - Calls payment-service to initiate payment.
3. payment-service:
   - Talks to external gateway.
   - Publishes `payment.success` or `payment.failed`.
4. order-service consumes payment events:
   - On success:
     - Mark order PAID.
     - Confirm stock.
     - Publish `order.paid`.
   - On failure:
     - Mark order CANCELLED.
     - Release stock reservation.
     - Publish `order.cancelled`.

Notification-service listens and sends appropriate emails/SMS.

---

## 5. Security Flow (JWT)

- auth-service issues JWT containing:
  - `sub` (user ID)
  - `email`
  - `roles`
- Gateway validates JWT on each request:
  - Rejects invalid/expired tokens.
  - Forwards authenticated requests with user info headers to backend services.

Backend services:
- Trust gateway as the boundary.
- Optionally re-validate token for critical endpoints.
