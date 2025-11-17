# Runbook: Alerts, Monitoring & Observability

## Purpose
Defines how PolyShop services are monitored, how alerts are configured, and how engineers
respond to system abnormalities.

Applies to:
- Auth
- Product
- Inventory
- Order
- Payment
- Notification
- Search
- Gateway

---

# 1. Observability Stack Overview

PolyShop uses:

| Layer | Tool | Purpose |
|-------|------|---------|
| Metrics | Prometheus | Scrapes metrics from each service |
| Dashboards | Grafana | Visual dashboards & alert rules |
| Logs | Loki | Centralized log aggregation |
| Tracing | Jaeger / OpenTelemetry | Distributed request tracing |
| Synthetic Tests | k6 | Automated performance checks |
| API Tests | Newman/Postman | Functional & regression tests |

---

# 2. Metrics Exposed By Each Service

All services expose `/actuator/prometheus` (Spring Boot) or `/metrics` (Node/Python).

## 2.1 Standard Metrics
- `http_server_requests_seconds_*`  
- `jvm_memory_used_bytes`  
- `cpu_usage_seconds`  
- `db_connection_pool_active`  
- `kafka_consumer_lag`  

## 2.2 Custom Business Metrics
Examples:
- Order Service → `orders_created_total`
- Payment Service → `payments_success_total`, `payments_failed_total`
- Inventory Service → `inventory_reservations_count`
- Auth Service → `logins_total`, `failed_logins_total`

---

# 3. Grafana Dashboards

Prepared dashboards:

### 3.1 Core Services Overview
- CPU/RAM
- HTTP latency p50/p90/p99
- Error rates (4xx/5xx)
- DB connection pool usage
- Kafka lag

### 3.2 Payment Dashboard
- Success/Failed payments
- Provider latency
- Refunds per hour

### 3.3 Order Saga Dashboard
- Saga step timings
- Compensation triggers
- In-flight sagas

### 3.4 Inventory Dashboard
- Stock movements per product
- Reservation success/failure
- Reservation TTL expirations

---

# 4. Alerting Rules

Alerts are defined in Prometheus or Grafana.

## 4.1 Severity Levels

### Critical (PagerDuty / on-call)
- Service unreachable  
- 5xx rate > 5% for > 2 minutes  
- Payment provider failures > 10/min  
- Kafka consumer lag > 20k messages  
- DB connection pool saturated (>= 90%)  

### Warning (Slack only)
- 4xx spikes  
- Latency p99 > 2 seconds  
- Increased inventory reservation failures  
- Slow search indexing  

### Info
- Deployments completed  
- Feature flags toggled  

---

# 5. Steps to Follow When an Alert Triggers

## Step 1 — Confirm Alert Validity
- Check dashboards  
- Check service logs in Loki  
- Compare with deployments timeline  

## Step 2 — Identify Affected Components
Use:
- `X-Request-Id`
- `traceId` span search
- Service name + error code

## Step 3 — Determine Severity
- Customer impact?
- Payment impact?
- Data loss?

## Step 4 — Investigate Root Cause
Typical actions:
- Check service logs  
- Inspect DB connections  
- Inspect Kafka consumer lag  
- Verify upstream/downstream services  

## Step 5 — Apply Recovery Procedure
Examples:
- Restart pod  
- Scale replicas  
- Trigger retries  
- Failover to secondary provider (payment/email)  

## Step 6 — Resolve and Close Alert
- Confirm alert stopped firing  
- Re-run synthetic tests  

## Step 7 — Document Incident
Update:
- Incident log  
- PagerDuty incident  
- ADR if system change required  

---

# 6. Log Monitoring: Loki Queries

Common queries:

**5xx errors**
```

{app="order-service"} |= "500"

```

**Search reserved stock failing**
```

{app="inventory-service"} |= "INSUFFICIENT_STOCK"

```

**Payment failures**
```

{app="payment-service"} |= "FAILED"

```

**High latency**
```

{app="gateway"} |= "slow" | json | duration > 2s

```

---

# 7. Tracing Rules

Each request must contain:
- `X-Request-Id`
- OpenTelemetry trace headers:
  - `traceparent`
  - `tracestate`

Jaeger UI:
- Displays complete request flow across services  
- Helps track stuck sagas  
- Helps debug cross-service errors  

---

# 8. Synthetic Monitoring

## k6 Runs Every 5 Minutes:
- `/auth/login`
- `/products?page=1`
- `/search/products?q=shoes`
- `/orders` creation flow (mock mode)
- `/payments` (sandbox mode)

Failures generate warnings.

---

# 9. Alert Suppression Rules

Suppress alerts during:
- Deployments
- Database migrations
- Known maintenance windows

---

# 10. When to Escalate

### Immediately escalate if:
- Payment success rate drops < 95%
- Order creation fails repeatedly
- Inventory reservation fails system-wide
- Search indexing stops updating

### Escalate to SRE Lead if:
- Multi-service outages  
- Kafka cluster instability  
- DB performance degradation lasting > 10 min  

---

# 11. End of Runbook

This is the authoritative reference for monitoring & alerts.