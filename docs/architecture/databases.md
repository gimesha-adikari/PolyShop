# PolyShop – Database Design (Per Service)

Each microservice owns its own PostgreSQL schema/database.
No direct cross-service DB access is allowed.

---

## Auth Service – auth_db

Tables:
- `users`
  - id (PK)
  - email (unique)
  - password_hash
  - name
  - created_at
  - updated_at

- `roles`
  - id (PK)
  - name (unique)

- `user_roles`
  - user_id (FK → users.id)
  - role_id (FK → roles.id)

- `refresh_tokens` (optional)
  - id
  - user_id
  - token
  - expires_at
  - revoked

---

## Product Service – product_db

Tables:
- `products`
  - id (PK)
  - name
  - description
  - price
  - currency
  - category_id (FK → categories.id)
  - brand
  - active
  - created_at
  - updated_at

- `categories`
  - id (PK)
  - name
  - parent_id (nullable, FK → categories.id)

- `product_images`
  - id (PK)
  - product_id (FK → products.id)
  - url

---

## Inventory Service – inventory_db

Tables:
- `stock`
  - id (PK)
  - product_id
  - available
  - reserved
  - updated_at

- `reservations`
  - id (PK)
  - order_id
  - product_id
  - quantity
  - status (RESERVED, RELEASED, CONFIRMED)
  - created_at
  - updated_at

---

## Order Service – order_db

Tables:
- `orders`
  - id (PK)
  - user_id
  - total_amount
  - currency
  - status (CREATED, PENDING_PAYMENT, PAID, CANCELLED, FULFILLED)
  - created_at
  - updated_at

- `order_items`
  - id (PK)
  - order_id (FK → orders.id)
  - product_id
  - quantity
  - unit_price
  - total_price

---

## Payment Service – payment_db

Tables:
- `payments`
  - id (PK)
  - order_id
  - amount
  - currency
  - status (INITIATED, SUCCESS, FAILED)
  - provider
  - provider_ref
  - created_at
  - updated_at

---

## Notification Service – notification_db (optional)

Tables:
- `email_logs`
  - id (PK)
  - to_address
  - template
  - status (SENT, FAILED)
  - error_message (nullable)
  - metadata (JSON)
  - created_at

- `sms_logs`
  - id (PK)
  - phone_number
  - template
  - status
  - error_message
  - metadata (JSON)
  - created_at

---

## Analytics Service – analytics_db 

Tables:
- `sales_daily`
  - date (PK)
  - total_orders
  - total_revenue

- `sales_monthly`
  - year
  - month
  - total_orders
  - total_revenue

- `events_raw`
  - id (PK)
  - event_type
  - payload (JSON)
  - created_at
