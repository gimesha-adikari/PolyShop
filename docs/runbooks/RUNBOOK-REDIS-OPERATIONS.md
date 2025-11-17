# Runbook: Redis Operations & Troubleshooting
PolyShop uses Redis for:
- Caching frequently accessed data
- Session/token blacklists
- Rate-limiting (auth-service)
- Distributed locks (inventory & order saga)
- Short-lived job queues (optional)

---

# 1. Redis Architecture

## Environments:
- **Local** → single Redis instance (non-clustered)
- **Production** → Redis Cluster (3 masters, 3 replicas)
- Persistence:
  - AOF enabled
  - RDB snapshots every 60s

## Critical Keys
| Service | Keys | Notes |
|--------|------|-------|
| `auth-service` | `refresh:{token}`, `rl:{ip}`, `session:{userId}` | Revocation, rate limits, sessions |
| `inventory-service` | `lock:product:{id}` | Distributed locking for reservations |
| `order-service` | `saga:{orderId}` | Saga step tracking |
| `search-service` | `cache:search:{query}` | Optional caching layer |

---

# 2. Connecting to Redis

## CLI:
```

redis-cli -h localhost -p 6379

```

## List keys:
```

keys *

```

## Check memory usage:
```

info memory

```

---

# 3. Monitoring Redis

### Key metrics to watch:
- Memory usage
- Evictions
- CPU load
- Connected clients
- Keyspace hits/misses
- Blocked clients
- Replication offsets

### Prometheus queries:
```

redis_memory_used_bytes
redis_evicted_keys_total
redis_connected_clients

```

### Grafana dashboards:
- Cache Hit Rate
- Slow Commands
- Ops/sec
- Replication Lag

---

# 4. Failure Scenarios & Recovery

## 4.1 High Memory Usage
**Symptoms:**
- Eviction spikes
- Performance drops

**Actions:**
1. Enable LRU eviction:
```

maxmemory-policy allkeys-lru

```
2. Increase memory or cluster nodes
3. Audit large keys:
```

memory usage <key>

```

---

## 4.2 Replication Lag
**Symptoms:**
- Stale reads from replicas
- Failover instability

**Actions:**
1. Inspect:
```

info replication

```
2. Restart replicas if stuck
3. Fix network bottlenecks

---

## 4.3 Redis Down
**Symptoms:**
- Authentication fails
- Rate limiting errors
- Order Saga stuck

**Actions:**
1. Restart pod:
```

kubectl rollout restart deploy redis

```
2. Check persistent volume
3. Validate cluster status:
```

redis-cli cluster info

```

---

## 4.4 Key Corruption or Flush
**Symptoms:**
- Tokens invalidated
- Caches cold
- Sagas losing state

**Recovery Strategy:**
1. Restore AOF backup
2. If cluster: resync from stable replica
3. Rebuild any caches lazily

---

# 5. Distributed Locking Runbook

Inventory & orders use Redis locks:

### Acquire lock:
```

SET lock:product:123 "uuid" NX EX 10

```

### Release lock:
```

if redis.get(key) == uuid then redis.del(key)

```

**Issues:**
- Lock stuck → EXPIRE ensures cleanup
- Overlapping locks → use unique UUID per request

---

# 6. Rate Limiting Runbook

Auth service uses the key:
```

rl:<ip>:<minute>

```

### If users being rate-limited incorrectly:
1. Check TTL:
```

ttl rl:123.123.42.55:20250129T12:41

```
2. Reset key:
```

del rl:123...

```

---

# 7. Backup & Restore

## Backup:
```

redis-cli save

```

## Restore:
1. Stop Redis
2. Replace `dump.rdb`
3. Restart

For AOF:
- Use `redis-check-aof --fix`

---

# 8. Security

- Password-auth enabled for production
- Redis only exposed inside cluster
- TLS termination if exposed externally
- Sensitive data stored with TTL (never permanent)

---

# 9. When to Escalate

### Immediate escalation:
- Cluster has < 2 masters online
- Memory usage > 85%
- Replica lag > 30 seconds
- Persistent failover loops

### Notify SRE Lead if:
- Repeated AOF corruptions
- Multiple flushed DB events
- Unauthorized access attempts

---

# 10. End of Runbook

This is the authoritative reference for Redis cluster operations in PolyShop.
