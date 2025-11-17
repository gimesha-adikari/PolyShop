# PolyShop – Outbox & Messaging Runtime Design

Version: **1.0.0**
Related docs:

* Section A – HTTP API Specs (`libs/api-schemas/*.yaml`)
* Section B – Event Contract (`polyshop-events.md`)

---

## 1. Goals and Scope

This document specifies how PolyShop implements:

* Reliable publishing of domain events to Kafka (or another broker)
* The **Outbox Pattern** in each service
* Consumer idempotency
* Dead-letter handling and retries
* Common responsibilities for each language stack (Java, Node.js, Python)

This is a **design document**, not implementation code.

Applies to services:

* Java: `auth-service`, `product-service`, `inventory-service`, `order-service`
* Node.js: `payment-service`
* Python: `notification-service`
* Consumers only: `search-service`, `analytics-service` (they don’t publish domain events initially)

---

## 2. Outbox Pattern – Conceptual Overview

### 2.1 Why Outbox

We want:

* DB state and events to be **atomically consistent**
* No “order saved but event not published” or vice versa
* Simple reasoning about eventual consistency

Solution: every **state change that should emit an event** writes to:

1. The domain tables (orders, payments, products, etc.)
2. An **outbox table** in the same transaction

A separate **Outbox Worker** publishes events from the outbox table to Kafka and marks them as processed.

### 2.2 Outbox Table (Conceptual Schema)

Each service has its own `outbox_events` table, co-located with its own DB.

Columns (logical):

* `id` – UUID, primary key
* `aggregate_id` – UUID/string referencing entity (orderId, userId, etc.)
* `event_type` – e.g. `order.order.placed.v1`
* `version` – integer, normally **1** to match event version
* `payload` – JSON, **matches Section B data** (no envelope)
* `created_at` – timestamp
* `processed` – boolean (default: `false`)
* `processed_at` – timestamp (nullable)
* `error_message` – nullable string, diagnostic only (for temporary failures)

Envelope (`eventId`, `traceId`, etc.) is built by the **publisher** when it reads from this table and sends to Kafka.

---

## 3. End-to-End Flow with Outbox

### 3.1 “Create Order” Example

1. Client POST `/orders` via gateway
2. `order-service`:

    * Validates request
    * Writes `orders` + `order_items` tables
    * Inserts row in `outbox_events`:

        * `event_type = "order.order.placed.v1"`
        * `aggregate_id = orderId`
        * `payload = { orderId, userId, items, totalAmount, ... }`
    * Commits transaction
3. Outbox worker (background component):

    * Reads unprocessed rows (e.g. `WHERE processed = false ORDER BY created_at LIMIT 100`)
    * For each row:

        * Wraps in **global event envelope** (from Section B)
        * Sends to Kafka topic `order.order.placed.v1`
        * Marks row `processed = true`, sets `processed_at`
4. Consumers (inventory, payment, notification, analytics) read event and act.

If worker crashes mid-batch, unprocessed rows remain `processed = false` and will be retried safely.

---

## 4. Outbox Worker Design

### 4.1 Responsibilities

Each producing service must have an **OutboxPublisher** worker with these responsibilities:

* Continuously poll `outbox_events`
* Publish events to broker with envelope:

    * `eventId` = outbox row `id`
    * `timestamp` = `created_at`
    * `eventType` = `event_type`
    * `source` = service name (`auth-service`, `order-service`, etc.)
* Use `aggregate_id` as Kafka key (for per-entity ordering)
* Mark events as processed only on successful send
* Handle retries and backoff for temporary failures

### 4.2 Main Algorithm (Pseudo-code)

Pseudo-code, language-agnostic:

```text
loop forever:
  begin tx
    rows = select * from outbox_events
           where processed = false
           order by created_at
           limit BATCH_SIZE
    if rows is empty:
      commit tx
      sleep IDLE_SLEEP_MS
      continue
  commit tx

  for each row in rows:
    try:
      envelope = buildEnvelope(row)
      kafkaProducer.send(topic=row.event_type, key=row.aggregate_id, value=envelope)
      markProcessed(row.id)
    catch TemporaryError:
      log error
      sleep RETRY_SLEEP_MS
    catch PermanentError:
      store error_message, optionally mark processed to avoid infinite loop
```

`buildEnvelope(row)` creates object like:

```json
{
  "eventId": "row.id",
  "timestamp": "row.created_at",
  "eventType": "row.event_type",
  "version": "row.version",
  "source": "service-name",
  "data": "row.payload",
  "metadata": {
    "traceId": "...",
    "correlationId": "...",
    "idempotencyKey": "...",
    "userId": "..."
  }
}
```

---

## 5. Per-Service Outbox Integration Plan

### 5.1 Java Services (auth, product, inventory, order)

Shared library: `libs/common-java`

Components to design (names indicative):

* `OutboxEvent` – JPA entity mapped to `outbox_events`
* `OutboxEventRepository` – Spring Data repository
* `OutboxPublisher` – scheduled component performing polling and publishing
* `DomainEventFactory` – helper to construct `payload` from domain entities
* `EventEnvelopeBuilder` – wraps payload into standard envelope

Lifecycle in a service:

1. In a service method (e.g. `OrderService.createOrder`):

    * Persist entity with JPA
    * Create `OutboxEvent` entity with `event_type` + JSON payload
    * Save both within a single transaction

2. `OutboxPublisher` (Spring `@Scheduled` or dedicated thread):

    * Fetch, publish, mark processed

You will reuse **the same common-java outbox components** in all Java services; only `source` and DB are different.

---

### 5.2 Node.js Payment Service

Directory: `services/payment-service`

Outbox design:

* DB: same Postgres instance used by payment-service (or a separate one if you decide so later)
* Table: `outbox_events` with same columns

Components (conceptual):

* `outboxRepository.js` – CRUD for `outbox_events`
* `paymentDomainService.js` – writes both payment row and outbox row in one transaction
* `outboxWorker.js` – background process:

    * Can be run as separate Node process or a repeated job (setInterval/bull queue)
    * Reads unprocessed events, publishes to Kafka, marks processed

When implementing payments:

* On successful status change from `INITIATED` → `SUCCESS`, insert outbox event:

    * `event_type = "payment.payment.succeeded.v1"`
    * `payload` = data defined in Section B

---

### 5.3 Python Notification Service

Directory: `services/notification-service`

Outbox usage is optional; initially, notification-service mainly **consumes** events.
If later it needs to emit its own domain events (like `notification.email.sent.v1`):

* Use SQLAlchemy (or your chosen ORM) to define `OutboxEvent` table
* When an email/SMS is successfully sent, insert an outbox row for `notification.email.sent.v1`
* A Python `outbox_worker.py`:

    * Periodically scans outbox table
    * Publishes to Kafka
    * Marks processed

For consumption (more important now), notification-service will:

* Listen to topics from Section B:

    * `order.order.placed.v1` → order confirmation email
    * `payment.payment.succeeded.v1` → payment receipt email
* Apply consumer idempotency (Section 6)

---

### 5.4 Search & Analytics Services

`search-service` and `analytics-service` are **consumers only** (for now):

* They DO NOT use outbox—they only read events from Kafka
* They maintain their own `processed_events` for idempotency (Section 6)

---

## 6. Consumer Idempotency Design

### 6.1 Per-Service Processed Events Store

Each consumer service (inventory, payment, notification, search, analytics) maintains a small table:

`processed_events`:

* `event_id` – UUID (from event envelope), primary key
* `processed_at` – timestamp

Optionally:

* `event_type`
* `source`

### 6.2 Consumer Algorithm

For any subscribed event:

1. Parse envelope, extract `eventId`, `eventType`, `data`
2. Open transaction
3. Check `processed_events` for `eventId`

    * If exists → **do nothing and commit** (duplicate)
4. Otherwise:

    * Apply domain logic (e.g. adjust stock, send email)
    * Insert `processed_events` row with `eventId`
5. Commit transaction

If crash occurs after domain change but before inserting `processed_events`, you might reprocess the same event; domain logic must be **idempotent**:

* Inventory: avoid double decrement by checking current state or storing movementId
* Notification: avoid sending duplicate emails by storing `notificationId` or status

---

## 7. Dead-Letter Topics and Error Handling

### 7.1 Dead-Letter Topic Convention

For each topic:

```
{topicName}.dlt
```

Examples:

* `order.order.placed.v1.dlt`
* `payment.payment.succeeded.v1.dlt`

### 7.2 When to Send to DLT

Consumer logic:

* If error is **temporary** (DB timeout, network):

    * Retry a few times with exponential backoff
* If error is **permanent** (validation error, impossible state):

    * Publish event to `{topic}.dlt` with:

        * Original envelope as `data.originalEvent`
        * Error details

Example DLT payload:

```json
{
  "eventId": "originalEventId",
  "originalEvent": {},
  "errorMessage": "Validation failed: product not found",
  "failedAt": "ISO-8601",
  "consumerService": "inventory-service"
}
```

Operations / runbook will monitor these DLT topics.

---

## 8. Topic Keys and Ordering

To guarantee sensible ordering:

* **auth events** keyed by `userId`
* **product events** keyed by `productId`
* **inventory events** keyed by `productId` or `variantId`
* **order events** keyed by `orderId`
* **payment events** keyed by `paymentId` or `orderId`
* **notification events** keyed by `notificationId`

This ensures that, for a particular entity, all events are seen in order by a consumer.

---

## 9. Observability & Tracing

Each event envelope (see Section B) includes:

* `eventId` – unique per event
* `traceId` – spans HTTP and events
* `correlationId` – groups related operations (e.g., order + inventory + payment)

You will configure:

* OpenTelemetry instrumentation in gateway + Java services
* Export traces to **Jaeger** (see `monitoring/jaeger/`)
* Logs always include `traceId` and `eventId` for debugging cross-service flows.

---

## 10. Summary of Responsibilities per Service

| Service              | Produces Events  | Uses Outbox | Consumes Events | Uses processed_events |
| -------------------- | ---------------- | ----------- | --------------- | --------------------- |
| auth-service         | Yes              | Yes         | No              | Yes (if needed)       |
| product-service      | Yes              | Yes         | No              | Yes (if needed)       |
| inventory-service    | Yes              | Yes         | Yes             | Yes                   |
| order-service        | Yes              | Yes         | Yes             | Yes                   |
| payment-service      | Yes              | Yes         | Yes             | Yes                   |
| notification-service | Maybe (Phase 2+) | Maybe       | Yes             | Yes                   |
| search-service       | No (Phase 2)     | No          | Yes             | Yes                   |
| analytics-service    | Maybe            | Maybe       | Yes             | Yes                   |

---