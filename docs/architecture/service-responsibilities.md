# PolyShop – Service Responsibilities

## Gateway (Spring Cloud Gateway)
- Single entry point for all client traffic.
- Routes requests to backend services.
- Performs:
  - JWT validation (auth service as source of truth).
  - Basic rate limiting.
  - CORS handling.
  - Simple request/response logging.
- Contains no business logic or persistence.

---

## Auth Service (Java / Spring Boot)
**Owns**
- User accounts.
- Credentials (hashed passwords).
- Roles and permissions.
- Access tokens (JWT) and refresh tokens.

**Responsibilities**
- Register new users.
- Authenticate users (login).
- Issue JWT access & refresh tokens.
- Validate tokens (for gateway and other services).
- Provide user profile and roles.
- Handle password reset and email verification (future).

---

## Product Service (Java / Spring Boot)
**Owns**
- Products.
- Categories.
- Brands.
- Product attributes/variants.
- Product media URLs (images), not the actual files.

**Responsibilities**
- CRUD for products, categories, brands.
- Expose product listing for customers and admin.
- Expose product details, pricing, and basic availability.
- Publish product-related events (e.g. product.created, product.updated) to Kafka for:
  - Search indexing.
  - Analytics.

---

## Inventory Service (Java / Spring Boot)
**Owns**
- Current stock levels per product/variant.
- Stock reservations during checkout.

**Responsibilities**
- Increase/decrease stock.
- Reserve stock for a pending order.
- Release reservation when:
  - Payment fails.
  - Order is cancelled or times out.
- Provide stock status to order / product service.

---

## Order Service (Java / Spring Boot)
**Owns**
- Orders and order items.
- Checkout orchestration flow.

**Responsibilities**
- Accept order creation requests.
- Validate user and product info (via other services).
- Coordinate with:
  - Inventory service (reserve and confirm stock).
  - Payment service (initiate and verify payments).
- Manage order state:
  - CREATED → PENDING_PAYMENT → PAID → FULFILLED → CANCELLED.
- Publish order-related events (order.created, order.paid, order.cancelled).
- Implement saga-style compensation if payment or stock fails.

---

## Payment Service (Node.js)
**Owns**
- Payment attempts.
- Payment provider interactions (Stripe/PayPal/etc).

**Responsibilities**
- Initiate payment sessions for orders.
- Interact with external payment gateways.
- Handle payment status webhooks.
- Validate that amount/order matches.
- Publish payment events (payment.success, payment.failed).

---

## Notification Service (Python / FastAPI)
**Owns**
- Notification templates (email/SMS).
- Notification delivery logs.

**Responsibilities**
- Send transactional emails (order created, payment success, password reset, etc.).
- Send SMS (optional).
- Consume events from Kafka:
  - order.created
  - order.paid
  - payment.failed
- Retry failed notifications, log status.

---

## Search Service (Java or Python – optional)
**Owns**
- Search index for products.

**Responsibilities**
- Consume product events (product.created/updated/deleted).
- Update Elasticsearch/OpenSearch index.
- (If exposed to clients) Provide search API endpoints (by keyword, category, filters).

---

## Analytics Service (Python – optional)
**Owns**
- Analytical views and aggregated data (reports).
- ML models and predictions (if implemented).

**Responsibilities**
- Generate reports (sales, top products, revenue).
- Consume events from orders/payments to maintain analytics data.
- Expose REST endpoints for analytics dashboards.
- Optionally provide ML-powered forecasts (sales/revenue).
