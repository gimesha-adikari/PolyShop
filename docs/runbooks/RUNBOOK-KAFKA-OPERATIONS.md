# Runbook: Kafka Operations & Troubleshooting

## Purpose
Defines how engineers manage, monitor, and recover Kafka clusters used by PolyShop.
Covers topics, consumer groups, retention, partitions, failures, and recovery.

Kafka is used for:
- Order Saga events
- Stock reservations
- Payment status events
- Search indexing
- Notification fan-out

---

# 1. Kafka Cluster Overview

PolyShop uses Kafka with:
- 3 brokers (minimum)
- 3 ZooKeeper nodes (or KRaft mode if using Kafka ≥ 3.3)
- Internal topics replicated for HA:
  - `order.events`
  - `payment.events`
  - `inventory.reservations`
  - `product.index`
  - `notification.send`

Replication factor: **3**  
Min In-Sync Replicas: **2**

---

# 2. Topic Structure & Naming

| Purpose | Topic Name | Notes |
|---------|------------|-------|
| Order Saga events | `order.events` | Source of truth for saga |
| Payment updates | `payment.events` | Provider → Payment svc |
| Inventory updates | `inventory.events` | Stock & reservations |
| Search indexing | `product.index` | Async indexing |
| Notifications | `notification.send` | Email/SMS dispatch |

---

# 3. How to Inspect Kafka Topics

## List topics
```

kafka-topics.sh --bootstrap-server localhost:9092 --list

```

## Describe a topic
```

kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order.events

```

## View messages
```

kafka-console-consumer.sh
--bootstrap-server localhost:9092
--topic order.events
--from-beginning

```

---

# 4. How to Check Consumer Lag

```

kafka-consumer-groups.sh
--bootstrap-server localhost:9092
--group inventory-service
--describe

```

If lag > **20,000**, alert triggers.

---

# 5. Common Issues & Resolutions

## 5.1 Consumer Lag Growing
**Symptoms:**
- Dashboards show increasing lag
- Orders stuck in saga steps
- Search indexing delayed

**Actions:**
1. Scale consumers:
```

kubectl scale deploy inventory-service --replicas=4

```
2. Check thread deadlocks
3. Restart consumer pods
4. Check for slow DB writes

---

## 5.2 Broker Unavailable
**Symptoms:**
- 5xx errors across services
- Producers throw `TimeoutException`
- Degraded throughput

**Actions:**
1. Check broker logs:
```

kubectl logs kafka-0

```
2. Restart unhealthy broker
3. Ensure disk not full
4. Validate network/port 9092

---

## 5.3 “NOT_ENOUGH_REPLICAS” Errors
**Cause:** ISR < min ISR.

**Actions:**
1. Inspect ISR:
```

kafka-topics.sh --describe --topic order.events

```
2. Restart sync-lagging brokers
3. Confirm replication factor >= 3

---

## 5.4 Message Duplication
Kafka guarantees **at-least-once** delivery.

Fix:  
→ Make consumers **idempotent** (use event IDs).

---

## 5.5 Dead Letter Queue Behaviour

Each main topic has a DLQ:
- `order.events.dlq`
- `inventory.events.dlq`
- `payment.events.dlq`

Messages land here when:
- Schema mismatch  
- Repeated consumer failure  
- Processing logic throws unrecoverable errors

**Action:**  
Review DLQ every day.

---

# 6. How to Reprocess DLQ Messages

```

kafka-console-consumer.sh
--bootstrap-server localhost:9092
--topic order.events.dlq
--from-beginning
--property print.key=true

```

Then manually re-publish:

```

kafka-console-producer.sh
--bootstrap-server localhost:9092
--topic order.events

```

---

# 7. Scaling Kafka

Scale brokers:
```

kubectl scale statefulset kafka --replicas=5

```

Scale partitions:
```

kafka-topics.sh --bootstrap-server localhost:9092 --alter
--topic order.events --partitions 12

```

---

# 8. Backup & Restore

## Backup
```

kafka-dump-log.sh --files /var/lib/kafka/data/order.events-0/00000001.log

```

## Restore  
Replay events from log dumps back to Kafka.

---

# 9. Security

- SASL/SCRAM enabled in production
- TLS required for external clusters
- Producer & Consumer ACLs:
  - `order-service` → write to `order.events`
  - `inventory-service` → consume from `order.events`
  - `search-service` → write/read `product.index`

---

# 10. When to Escalate

### Escalate immediately if:
- Two brokers go DOWN
- ISR drops below 2 for any topic
- Payment events stop flowing
- Order sagas stuck > 5 minutes

### Escalate to SRE Lead if:
- Partition reassignment repeatedly failing
- Corrupted log segments
- ZooKeeper / KRaft instability

---

# 11. End of Runbook

This is the authoritative reference for Kafka operations in PolyShop.
