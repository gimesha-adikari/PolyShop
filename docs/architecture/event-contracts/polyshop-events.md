# **POLYSHOP – EVENT CONTRACT SPECIFICATION**

Version: **1.0.0**
Scope: **All Async Events (Kafka), Schemas, Topics, Producers, Consumers, Versioning, Delivery, Immutability, Error/Retry Logic, Outbox Pattern**.

---

# **1. Event Architecture Overview**

PolyShop uses **event-driven microservices**, where each service publishes domain events that other services consume.

Design principles:

1. **Events are immutable**
2. **Events describe something that ALREADY happened**
3. **Events are append-only**
4. **Consumers act independently; producers don’t wait**
5. **Messages are versioned** (never breaking changes)
6. **Guaranteed delivery** using **Outbox Pattern + Kafka**
7. **Idempotent consumers** (avoid double processing)

Event format is standardized using JSON Schema.

---

# **2. Kafka Topic Naming Convention**

Format:

```
{domain}.{entity}.{eventName}.v{version}
```

Examples:

```
auth.user.created.v1
product.product.created.v1
order.order.placed.v1
payment.payment.succeeded.v1
inventory.stock.reserved.v1
```

Reasons:

* Easy filtering by domain
* Versioning is built into the topic name (supports parallel versions)
* Tools like Kafka UI show clear separation

---

# **3. Global Event Envelope**

Every published event MUST use this standard envelope:

```json
{
  "eventId": "uuid",
  "timestamp": "ISO-8601",
  "eventType": "string",
  "version": 1,
  "source": "microservice-name",
  "data": {},             
  "metadata": {
    "traceId": "uuid",
    "correlationId": "uuid",
    "idempotencyKey": "string (optional)",
    "userId": "string (optional)"
  }
}
```

Why?

* Consistent observability
* Easy tracing across services
* Supports distributed transactions

---

# **4. Domain Events by Microservice**

Below are all events grouped by service.
Each event includes:

* Topic name
* Who produces it
* Who consumes it
* JSON schema
* When it is triggered

---

# **4.1 AUTH SERVICE EVENTS**

### **1. User Registered**

**Topic:** `auth.user.created.v1`
**Produced by:** auth-service
**Consumed by:**

* notification-service (send welcome email)
* analytics-service (new user metrics)

**Schema:**

```json
{
  "userId": "uuid",
  "email": "string",
  "fullName": "string",
  "createdAt": "ISO-8601"
}
```

### **2. User Email Verified**

**Topic:** `auth.user.verified.v1`
Consumers: analytics-service

### **3. User Roles Updated**

**Topic:** `auth.user.role-updated.v1`

---

# **4.2 PRODUCT SERVICE EVENTS**

### **1. Product Created**

**Topic:** `product.product.created.v1`
Consumers:

* search-service (index product)
* analytics-service
* notification-service (optional)

Schema:

```json
{
  "productId": "uuid",
  "name": "string",
  "price": "number",
  "categoryId": "uuid",
  "createdAt": "ISO-8601"
}
```

---

### **2. Product Updated**

**Topic:** `product.product.updated.v1`
Consumers:

* search-service (update search index)
* analytics-service

### **3. Product Deleted**

**Topic:** `product.product.deleted.v1`
Consumers:

* search-service

### **4. Variant Created / Updated / Deleted**

Topics:

* `product.variant.created.v1`
* `product.variant.updated.v1`
* `product.variant.deleted.v1`

Consumers:

* inventory-service (adjust variant stock structures)
* search-service

---

# **4.3 INVENTORY SERVICE EVENTS**

### **1. Stock Movement Created**

**Topic:** `inventory.stock.movement.v1`
Used by:

* analytics-service (inventory metrics)
* admin dashboards

Schema:

```json
{
  "movementId": "uuid",
  "productId": "uuid",
  "variantId": "uuid",
  "quantityDelta": "int",
  "reason": "string",
  "createdAt": "ISO-8601"
}
```

---

### **2. Reservation Created**

**Topic:** `inventory.stock.reserved.v1`
Consumers:

* order-service (validate reservation)
* payment-service (prepare payment)

### **3. Reservation Confirmed**

**Topic:** `inventory.stock.confirmed.v1`

### **4. Reservation Released**

**Topic:** `inventory.stock.released.v1`
Used by:

* analytics-service
* admin reporting

---

# **4.4 ORDER SERVICE EVENTS**

### **1. Order Placed**

**Topic:** `order.order.placed.v1`
Consumers:

* inventory-service (reserve stock)
* payment-service (create payment intent)
* notification-service (order email)
* analytics-service

Schema:

```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "variantId": "uuid",
      "quantity": "int"
    }
  ],
  "totalAmount": "number",
  "currency": "string",
  "createdAt": "ISO-8601"
}
```

---

### **2. Order Paid**

**Topic:** `order.order.paid.v1`
Consumers:

* inventory-service (confirm reservation)
* notification-service (receipt email)
* analytics-service

### **3. Order Cancelled**

**Topic:** `order.order.cancelled.v1`
Consumers:

* inventory-service (release reservation)
* analytics-service

---

# **4.5 PAYMENT SERVICE EVENTS**

### **1. Payment Succeeded**

**Topic:** `payment.payment.succeeded.v1`
Consumers:

* order-service (move to PAID)
* inventory-service (confirm reservation)
* notification-service (payment email)

Schema:

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": "number",
  "currency": "string",
  "provider": "string",
  "createdAt": "ISO-8601"
}
```

### **2. Payment Failed**

**Topic:** `payment.payment.failed.v1`

---

### **3. Refund Created**

**Topic:** `payment.refund.created.v1`
Consumers:

* analytics-service
* notification-service

---

# **4.6 NOTIFICATION SERVICE EVENTS**

### **1. Email Sent**

**Topic:** `notification.email.sent.v1`
Used for:

* email reliability tracking
* analytics-service

Schema:

```json
{
  "notificationId": "uuid",
  "type": "EMAIL",
  "template": "string",
  "sentAt": "ISO-8601"
}
```

### **2. Email Failed**

**Topic:** `notification.email.failed.v1`

---

# **4.7 SEARCH SERVICE EVENTS**

Search consumes events. It usually **does not produce** events.
Optional:

* `search.index.rebuild.completed.v1`

---

# **4.8 ANALYTICS SERVICE EVENTS**

Analytics consumes everything and **may produce derived events**:

* `analytics.daily-report.ready.v1`
* `analytics.user-activity.v1`

These events are not mandatory for phase 1.

---

# **5. Event Versioning Strategy**

### **Rules**

1. **Breaking change → bump version → new topic**
   Example:

   ```
   product.product.created.v2
   ```

2. **Non-breaking change → same version**
   Add optional field → OK
   Add metadata → OK

3. Consumers choose which version to subscribe to.

---

# **6. Delivery, Ordering & Retry Rules**

### **Delivery Guarantee**

**At-least-once** delivery using:

* Kafka
* Outbox Pattern in DB
* Consumer idempotency

### **Ordering**

Events are partitioned by entity (e.g., productId):

```
key = productId → all product events in order
key = orderId → all order events ordered
```

### **Retries**

Consumers retry with exponential backoff.

If still failing:

```
event → dead-letter-topic (DLT)
```

### **Dead Letter Topic Convention**

```
{original-topic}.dlt
```

Example:

```
order.order.placed.v1.dlt
```

---

# **7. Outbox Pattern Specification**

Every service that writes events must use an **outbox table**:

```
outbox_events (
    id uuid PK,
    aggregate_id uuid,
    event_type text,
    version int,
    payload jsonb,
    created_at timestamp,
    processed boolean default false
)
```

Flow:

1. Service writes DB transaction (e.g., create order)
2. Same transaction inserts event into `outbox_events`
3. Background worker reads unprocessed outbox rows
4. Publishes to Kafka
5. Marks row processed

Guarantees:

* Atomic write
* Exactly-once publication (even on retry)

---

# **8. Event Security**

Kafka events may contain sensitive fields.

Rules:

* Never include passwords / tokens
* Mask PII fields when possible
* Use JWT `sub` for identifying the user
* Optionally sign events using HMAC or RSA

---

# **9. Event Traceability**

Each event has:

```
traceId → generated at gateway  
correlationId → generated for multi-step flows  
eventId → unique per event
```

Tracing uses:

* OpenTelemetry
* Jaeger / Zipkin

---

# **10. Event Flow Diagrams**

## **10.1 Order Placement Flow (Text Diagram)**

```
User → API Gateway → Order Service
Order Service → (Kafka) order.order.placed.v1
    ↳ Inventory Service (reserve stock)
    ↳ Payment Service (create intent)
    ↳ Notification Service (order email)
    ↳ Analytics Service (metrics)

Inventory → (Kafka) inventory.stock.reserved.v1
Payment → (Kafka) payment.payment.succeeded.v1
Order → moves to PAID
```

---

## **10.2 Payment Success Flow**

```
payment-service → payment.payment.succeeded.v1 →
    order-service (mark PAID)
    inventory-service (confirm reservation)
    notification-service (receipt email)
    analytics-service
```

---

## **10.3 Refund Flow**

```
payment-service → payment.refund.created.v1 →
    notification-service
    analytics-service
```

---

# **11. Complete Topic List (Summary)**

### **Auth**

* auth.user.created.v1
* auth.user.verified.v1
* auth.user.role-updated.v1

### **Product**

* product.product.created.v1
* product.product.updated.v1
* product.product.deleted.v1
* product.variant.created.v1
* product.variant.updated.v1
* product.variant.deleted.v1

### **Inventory**

* inventory.stock.movement.v1
* inventory.stock.reserved.v1
* inventory.stock.confirmed.v1
* inventory.stock.released.v1

### **Order**

* order.order.placed.v1
* order.order.paid.v1
* order.order.cancelled.v1

### **Payment**

* payment.payment.succeeded.v1
* payment.payment.failed.v1
* payment.refund.created.v1

### **Notification**

* notification.email.sent.v1
* notification.email.failed.v1

### **Analytics (Optional)**

* analytics.daily-report.ready.v1

---

# **12. Consumer Idempotency Rules**

Consumers must detect duplicates using:

```
processed_events (
    event_id uuid PK,
    processed_at timestamp
)
```

Steps:

1. Check if event_id exists
2. If yes → skip
3. If no → process & insert

---

# **13. Error, Retry & DLT Policy**

Policy:

| Error Type                      | Action                  |
| ------------------------------- | ----------------------- |
| Temporary (DB timeout, network) | Retry with backoff      |
| Permanent (validation error)    | Send to DLT immediately |
| Poison event                    | Send to DLT and alert   |

---

# **14. When to Emit Each Event (Lifecycle Table)**

| Action            | Event emitted                | Service   |
| ----------------- | ---------------------------- | --------- |
| user registers    | auth.user.created.v1         | auth      |
| product created   | product.product.created.v1   | product   |
| stock deducted    | inventory.stock.movement.v1  | inventory |
| order created     | order.order.placed.v1        | order     |
| payment succeeded | payment.payment.succeeded.v1 | payment   |
| order cancelled   | order.order.cancelled.v1     | order     |
