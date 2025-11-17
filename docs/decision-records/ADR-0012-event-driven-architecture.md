# ADR-0012: Event-Driven Architecture for PolyShop

## Status
Accepted

## Date
2025-11-17

## Context

PolyShop consists of multiple domain services:

- Auth
- Product
- Inventory
- Order
- Payment
- Search
- Notification
- Analytics

These services must interact without tight coupling. Synchronous HTTP communication alone causes:

- Cascading failures
- High latency during spikes
- Difficulty scaling specific components
- Hard-to-maintain dependencies
- No audit trail of domain events

PolyShop must support:

- Distributed transactions (Order Saga)
- Inventory reservations
- Payment confirmations
- Search reindex triggers
- Notification workflows
- Analytics pipeline ingestion

An event-driven architecture is required.

---

# Decision

PolyShop adopts a **Kafka-based event-driven architecture** with strongly typed event contracts stored in:

```

/docs/events/polyshop-events.md

````

Each service will publish domain events when state changes.  
Other services subscribe asynchronously.

Example:
- Order Service publishes `order.created`
- Inventory Service consumes it to create reservations
- Analytics consumes it for reporting
- Notification Service sends order confirmation email

---

# Event Categories

Events are grouped by domain:

1. **Auth Events**  
   `user.registered`, `user.email.verified`, `user.blocked`

2. **Product Events**  
   `product.created`, `product.updated`, `product.deleted`

3. **Inventory Events**  
   `stock.reserved`, `stock.released`, `stock.updated`

4. **Order Events**  
   `order.created`, `order.paid`, `order.cancelled`, `order.fulfilled`

5. **Payment Events**  
   `payment.initiated`, `payment.success`, `payment.failed`, `payment.refunded`

6. **Notification Events**  
   `email.sent`, `sms.sent`

7. **Search Events**  
   `product.index.requested`, `product.index.updated`

8. **Analytics Events**  
   `event.ingested`

---

# Event Model Rules

### Rule 1 — Events are immutable
Once published, payloads cannot be changed.

### Rule 2 — Events follow the shared format:

```json
{
  "eventId": "uuid",
  "type": "order.created",
  "version": 1,
  "timestamp": "2025-01-01T12:00:00Z",
  "traceId": "request-id",
  "source": "order-service",
  "data": { ... }
}
````

### Rule 3 — Services never depend on each other’s database schema

Only on events.

### Rule 4 — Consumers must be idempotent

Event replay must not break systems.

### Rule 5 — All events go into Dead-Letter Queue (DLQ) after N failures

DLQ topic pattern:

```
polyshop.dlq.<original-topic>
```

### Rule 6 — Events must be versioned

New versions require:

* Incrementing `version`
* Maintaining backward compatibility
* Deprecation notice in event contract file

---

# Topic Naming Convention

Format:

```
polyshop.<service>.<domain>.<action>.v<version>
```

Examples:

```
polyshop.order.created.v1
polyshop.inventory.stock.reserved.v1
polyshop.payment.success.v1
```

---

# Kafka Partition Strategy

### High-throughput events

Use multiple partitions:

* search indexing
* analytics ingestion

### Order-sensitive events

Use single-partition keyed events, e.g.:

```
key = orderId
partitioning ensures ordering
```

This is required for the Order Saga.

---

# Event Retention Rules

* Business events: **7 days**
* High-frequency analytics: **24 hours**
* Audit events: **90 days**
* DLQ: **14 days**

---

# Schema Management

Event schemas stored in:

```
/docs/events/
```

Formats supported:

* JSON Schema (primary)
* Avro (optional future)

Services must validate events before publishing.

---

# Alternatives Considered

### Option A — REST-only communication

Rejected due to cascading failures and poor decoupling.

### Option B — gRPC between services

Not sufficient for broadcast-style or fan-out processing.

### Option C — RabbitMQ

Good for queueing but weaker for event streaming and partitioning.

Kafka was selected due to:

* High throughput
* Persistence
* Perfect for analytics pipeline
* Multiple independent consumers
* Replay support
* Ordering guarantees

---

# Consequences

### Positive

* Full decoupling between services
* Replayable events
* Scalable and resilient workflows
* Clean Saga orchestration
* Search and analytics pipelines become trivial

### Negative

* Requires Kafka cluster
* More operational complexity
* Consumers must implement idempotency

---

# Final Decision

PolyShop adopts a **Kafka-based event-driven architecture** with:

* Strongly typed event contracts
* Versioned events
* DLQ handling
* Persistent logs
* Idempotent consumers

This architecture is now mandatory for all services.
