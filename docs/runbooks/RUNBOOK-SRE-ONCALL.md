# Runbook: SRE On-Call Procedures

## Purpose

Defines responsibilities, workflows, and escalation paths for engineers handling on-call duties for PolyShop’s platform.

This runbook applies to:

* Auth Service
* Product Service
* Inventory Service
* Order Service
* Payment Service
* Notification Service
* Search Service
* API Gateway
* Shared Infrastructure (Kafka, Redis, PostgreSQL, K8s)

---

# 1. On-Call Responsibilities

### Primary Responsibilities:

* Respond to alerts within SLA
* Diagnose service health issues
* Restore critical services
* Communicate status during incidents
* Coordinate with backend and DevOps teams
* Document incidents

### SLA:

| Severity | Response Time | Resolution Target |
| -------- | ------------- | ----------------- |
| SEV-1    | < 5 minutes   | < 30 minutes      |
| SEV-2    | < 10 minutes  | < 2 hours         |
| SEV-3    | < 30 minutes  | Next 24 hours     |

---

# 2. Alert Sources

On-call receives alerts from:

* Grafana Alerting
* Prometheus Alertmanager
* Loki log alerts
* Synthetic tests (k6)
* Payment provider webhooks
* UptimeRobot external monitors

Alert channels:

* PagerDuty (critical)
* Slack `#alerts` (warnings)
* Email (info only)

---

# 3. Standard On-Call Flow

## Step 1 — Acknowledge Alert

* Acknowledge within SLA
* Silence duplicate alerts
* Check recent deployments

## Step 2 — Assess Impact

Determine:

* Affected service
* User impact
* Scope (partial or full outage)
* Data impact
* Dependencies (DB, Kafka, Redis, external)

## Step 3 — Investigate

Use tools:

* Logs: Loki
* Metrics: Grafana
* Traces: Jaeger
* K8s deployment status
* Kafka consumer lag
* DB connection status
* Recent event messages

## Step 4 — Apply Mitigation

Common fixes:

* Restart failing pods
* Scale service instances
* Fail over to backup provider
* Increase Kafka partitions
* Clear Redis locks or caches
* Temporarily disable new traffic via Gateway

## Step 5 — Communicate

Send updates every 10 minutes:

* What’s happening
* What was investigated
* What is being tried
* Estimated time to resolve

Communicate via:

* #incident channel
* PagerDuty incident timeline
* Stakeholders group

## Step 6 — Resolve & Verify

* Validate system is stable
* Ensure alerts stop
* Run synthetic tests
* Re-enable rate limits / toggles
* Confirm DB, Kafka, Redis normal

## Step 7 — Document Incident

Fill out:

* Incident report
* Root cause analysis
* Post-incident tasks
* ADR if architecture change required

---

# 4. Incident Severity Levels

### SEV-1 (Critical)

Examples:

* Payment failures > 10/min
* Order creation failing for all users
* Search service down
* Kafka broker down
* Auth login failing

Actions:

* Immediate PagerDuty wake-up
* Cross-team swarm
* Frequent communications
* Full RCA required

---

### SEV-2 (Major)

Examples:

* High latency on Gateway
* Inventory reservation delays
* Notification emails delayed
* Regional outage

Actions:

* Fast response (<10 min)
* Fix or mitigate
* Moderate urgency

---

### SEV-3 (Minor)

Examples:

* Slow admin APIs
* Partial feature degradation
* Scheduled maintenance issues

Actions:

* Can be deferred
* Logged for next sprint

---

# 5. On-Call Tools & Commands

### Kubernetes

```
kubectl get pods
kubectl logs <pod>
kubectl rollout restart deploy/<service>
kubectl top pods
```

### Kafka

```
kafka-consumer-groups.sh --describe
kafka-topics.sh --list
```

### Redis

```
redis-cli info memory
redis-cli keys *
```

### PostgreSQL

```
SELECT * FROM pg_stat_activity;
```

### Gateway Fail-Open / Fail-Closed

* Disable route temporarily
* Reroute to maintenance page

---

# 6. Runbook for Common Issues

## 6.1 Payment Failures

* Check provider API status
* Switch to backup provider
* Queue retries

## 6.2 Kafka Lag

* Scale consumers
* Restart slow pods
* Check DB performance

## 6.3 Redis Lock Stuck

* Delete specific lock key
* Verify saga progression

## 6.4 Search Index Stalled

* Reprocess indexing topic
* Restart search workers

## 6.5 Auth Login Errors

* Check DB
* Check rate-limiting keys
* Inspect provider bans

---

# 7. Escalation Policy

### Escalate Immediately:

* Multi-service outage
* Payment success rate < 95%
* Kafka replication issues
* Redis cluster in failover loops
* DB CPU > 90% for > 5 minutes

### Escalate to:

* SRE Lead
* Backend Team Lead
* DevOps Engineer on-call

---

# 8. Handover Procedure

At shift end:

* List unresolved issues
* Link ongoing incidents
* Share expected triggers
* Transfer PagerDuty rotation
* Confirm recipient is active

---

# 9. On-Call Checklist

* [ ] PagerDuty active
* [ ] Laptop & VPN prepared
* [ ] Slack notifications enabled
* [ ] Access to Grafana, Jaeger, Kibana
* [ ] Access to K8s cluster
* [ ] Access to logs

---

# 10. End of Runbook

This is the official SRE on-call procedure for PolyShop.