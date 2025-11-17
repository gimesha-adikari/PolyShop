# Runbook: PostgreSQL Operations & Troubleshooting
PolyShop uses PostgreSQL as the primary OLTP database for all core services:
- auth-service
- product-service
- order-service
- inventory-service
- analytics-service (partially)

---

# 1. PostgreSQL Architecture

## Local Development:
- Single PostgreSQL container
- No replication
- No backups required (dev only)

## Production Deployment:
- 3-node HA setup (Patroni or RDS Multi-AZ)
- Streaming replication
- Automated failover + backups
- PITR (Point-In-Time Recovery)

## Storage:
- SSD volumes
- 30-day WAL retention
- Automated vacuum & analyze jobs

---

# 2. Connecting to PostgreSQL

### CLI:
```

psql -h localhost -p 5432 -U polyshop

```

### Show databases:
```

\l

```

### Show tables:
```

\dt

```

### Describe table:
```

\d table_name

```

---

# 3. Monitoring PostgreSQL

### Critical Metrics:
- CPU usage
- Disk I/O saturation
- Slow queries
- Replication lag
- Deadlocks
- Connection pool usage (HikariCP)
- Table bloat
- WAL generation rate

### Prometheus metrics (examples):
```

pg_stat_replication_lag
pg_locks_count
pg_stat_activity_total
pg_xact_commit_total
pg_xact_rollback_total
pg_stat_bgwriter_buffers_alloc

```

### Grafana dashboards:
- Query Performance
- Buffer Cache Hit Ratio
- Replication & WAL
- Connections & Locks

---

# 4. Common Operational Tasks

## 4.1 Restart Database
Kubernetes:
```

kubectl rollout restart statefulset postgres

```

Local:
```

docker restart postgres

```

---

## 4.2 Check Long-Running Queries
```

SELECT pid, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;

```

Kill query:
```

SELECT pg_terminate_backend(<pid>);

```

---

## 4.3 Check Replication Lag
```

SELECT * FROM pg_stat_replication;

```

If lag > 10s:
- Inspect network issues
- Restart replica
- Validate WAL configuration

---

## 4.4 Vacuum & Analyze
```

VACUUM (VERBOSE, ANALYZE);

```

Recommended:
- Autovacuum on
- Threshold tuning for high-write tables

---

# 5. Failure Scenarios & Recovery

## 5.1 Database Down
**Symptoms:**
- All services 500
- Saga failing
- Auth failing

**Actions:**
1. Restart DB pod
2. Validate PVC health
3. Check logs:
```

kubectl logs postgres-0

```

---

## 5.2 High Connection Usage
**Symptoms:**
- HikariCP timeouts
- “Too many connections”

**Actions:**
1. See active connections:
```

SELECT * FROM pg_stat_activity;

```
2. Increase connection pool size
3. Scale replicas of the service
4. Use PgBouncer for pooling

---

## 5.3 Deadlocks
**Symptoms:**
- Frequent 409/500 errors

**Actions:**
1. Inspect deadlocks:
```

SELECT * FROM pg_locks;

```
2. Add proper indexing
3. Reduce transaction scope
4. Reorder write statements

---

## 5.4 Slow Queries
1. Enable slow query log:
```

log_min_duration_statement = 500

```
2. Fetch EXPLAIN plans
3. Add indexes
4. Optimize joins
5. Denormalize hot fields (rare)

---

# 6. Backup & Restore

## Backups:
- Nightly full backups
- WAL archiving every 5 mins

## Restore PITR (Point-in-time):
1. Stop cluster
2. Restore base backup
3. Apply WAL logs to desired timestamp

---

# 7. Schema Migrations (Flyway)

Services use Flyway migrations in:
```

classpath:db/migration

```

Commands:
```

./gradlew flywayMigrate
./gradlew flywayInfo

```

Guidelines:
- Never modify old migrations
- Create new incremental files
- Always test on staging before prod

---

# 8. Security

- Enforce SSL/TLS connections
- Rotate DB passwords quarterly
- Enforce least-privilege roles:
  - `polyshop_api_user` → RW
  - `polyshop_readonly` → R
- Disable SUPERUSER for application roles

---

# 9. When to Escalate

Immediately escalate if:
- Replication lag > 30 seconds
- Primary failover loops
- Data corruption detected
- WAL archive stops working
- Disk free < 10%

---

# 10. End of Runbook

This runbook is the authoritative reference for PostgreSQL operations for PolyShop.