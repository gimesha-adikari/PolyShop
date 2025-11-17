# Runbook: Error Handling & Failure Recovery

## Purpose
This runbook defines how PolyShop engineers diagnose and resolve errors across all services
(Auth, Product, Inventory, Order, Payment, Notification, Search, Gateway).

It standardizes:
- how errors are logged  
- how errors are traced  
- how engineers respond  
- how to recover the system  

---

# 1. Error Categories

## 1.1 Client Errors (4xx)
- `400` – validation failure  
- `401` – unauthorized  
- `403` – forbidden  
- `404` – not found  
- `409` – conflict (idempotency violation, concurrency issues)  
- `429` – rate limiting  

**Always safe to return to client.**  
Not considered system incidents.

---

## 1.2 Server Errors (5xx)
Examples:
- DB connection failure  
- Kafka unavailable  
- Redis unavailable  
- Internal NPE / unhandled exception  

These **must be logged, traced, and monitored**.

---

## 1.3 Business Logic Errors
Common examples:
- “Stock unavailable” in Inventory Service  
- “Payment declined” in Payment Service  
- “Order cannot transition to state XYZ” in Order Service  

These are expected conditions, logged at **WARN**, not **ERROR**.

---

## 1.4 External Provider Errors
- Stripe down  
- Email/SMS provider failure  
- Search indexing timeout  

These failures are retriable and require fallback logic.

---

# 2. Error Logging Standards

Every service must log at minimum:

```

timestamp
level
serviceName
requestId
userId (if authenticated)
errorCode
message
stackTrace (only for ERROR)

````

### Mandatory:
- Include `X-Request-Id` in every log.
- Include `traceId` / `spanId` from OpenTelemetry.

---

# 3. Error Response Format

All services return:

```json
{
  "timestamp": "2025-02-20T09:30:00Z",
  "status": 500,
  "error": "INTERNAL_SERVER_ERROR",
  "code": "ORDER_DB_FAILURE",
  "message": "Failed to create order",
  "path": "/orders",
  "requestId": "d4f1a1bc-8a4a-44ef-b21a-13b8c2a29b30"
}
````

**Never expose:**

* stack traces
* DB names
* internal exceptions

---

# 4. Triage Process (When an Alert Triggers)

### Step 1 — Identify the failing service

Check alerts:

* Prometheus
* Grafana dashboard
* Loki logs
* Alertmanager notifications

### Step 2 — Locate the failing request

Search logs by:

* `requestId`
* `traceId`
* service name

### Step 3 — Classify the error

Which category?

* Client
* Server
* Business
* External provider

### Step 4 — Reproduce (if possible)

For 5xx errors:

* Re-run failing request in Postman
* Check OpenAPI spec
* Check DB state

### Step 5 — Resolve root cause

Examples:

* DB pool exhaustion → restart pod, bump pool size
* Kafka consumer stuck → restart consumer group
* Payment provider down → enable fallback provider

### Step 6 — Document the incident in Incident Report

---

# 5. Recovery Procedures

## 5.1 Database Down

1. Verify PostgreSQL pod is running
2. Check connections: `SELECT * FROM pg_stat_activity;`
3. Restart service deployment
4. Scale replicas to reduce load

## 5.2 Kafka Down

1. Check Zookeeper
2. Restart brokers
3. Clear consumer offset if stuck
4. Re-run failing service

## 5.3 Redis Down

1. Restart Redis
2. Clear corrupt shards
3. Service auto-rebuilds caches

## 5.4 Payment Provider Failure

* Mark payment as `REQUIRES_ACTION`
* Queue retry job
* Notify customer via Notification Service

## 5.5 Inventory Reservation Failures

* Release locks
* Rebuild stock table from movements ledger

---

# 6. Escalation Guidelines

### Severity 1 — Outage / Payments Failing

* Notify on-call engineer
* Notify team lead
* Post status-page message
* Resolve within 15 minutes

### Severity 2 — Partial Degraded Functionality

* Payment retries slow
* Search indexing delayed
* Resolve within 2 hours

### Severity 3 — Minor bugs

* Non-blocking
* Add to backlog

---

# 7. Tools Used in Error Handling

| Tool       | Purpose                       |
| ---------- | ----------------------------- |
| Grafana    | Metrics dashboards            |
| Loki       | Central log aggregation       |
| Jaeger     | Distributed tracing           |
| Prometheus | Alerts & metrics              |
| k6         | Load/performance verification |
| Newman     | API regression tests          |

---

# 8. Key Principles

* Fail fast
* Never hide errors
* Never log sensitive data
* Use correlation IDs everywhere
* Prefer retries with backoff
* Prefer compensating actions to deletes

---

# 9. When to Create a Follow-up ADR

Create an ADR when error handling required:

* New retry logic
* New fallback providers
* New compensating transactions
* New circuit breaker rules

---

# 10. End of Runbook

This is the canonical error-handling reference for PolyShop.