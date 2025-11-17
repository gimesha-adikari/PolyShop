# Runbook: Deployment Procedures & Release Management

## Purpose

Defines how PolyShop services are deployed, verified, rolled back, and monitored during production releases.

Applies to:

* Auth
* Product
* Inventory
* Order
* Payment
* Notification
* Search
* Gateway

---

# 1. Deployment Architecture

PolyShop uses:

| Layer             | Tool                           |
| ----------------- | ------------------------------ |
| CI                | GitHub Actions                 |
| Artifact Registry | GHCR or AWS ECR                |
| Deployments       | Kubernetes (Helm or Kustomize) |
| Canary            | Argo Rollouts                  |
| Monitoring        | Prometheus + Grafana           |
| Logs              | Loki                           |
| Tracing           | OpenTelemetry + Jaeger         |

---

# 2. Deployment Types

## 2.1 Standard Release

Triggered when:

* Code is merged into `main`
* Version tag is created: `v1.x.x`

Pipeline:

1. Build
2. Test
3. Security scan
4. Docker build & push
5. K8s deploy
6. Run smoke tests

---

## 2.2 Hotfix Release

Trigger:

* Critical bug
* Outage fix

Rules:

* Based on `release/*` or `hotfix/*` branch
* Must be ≤ 1 file change unless approved
* Minimal impact deployment

---

## 2.3 Canary Deployment

* 5% → 20% → 50% → 100% traffic
* Automatic rollback on error rate > 2%
* Observability checks run at each step

---

# 3. Standard Deployment Process

## Step 1 — Prepare Release

* Ensure PR is approved
* Ensure tests pass
* Update version
* Tag release:

```
git tag v1.3.0
git push origin v1.3.0
```

---

## Step 2 — CI Pipeline Runs

Pipeline runs:

1. **Unit tests**
2. **Integration tests**
3. **Security checks** (Snyk / Trivy)
4. **Build image**
5. **Push to registry**
6. **Apply manifests to Kubernetes**

Artifacts:

* Docker images
* SBOM
* Test reports

---

## Step 3 — Kubernetes Rollout

Command:

```
kubectl rollout status deploy/auth-service
```

Verify:

* No CrashLoopBackOff
* No ImagePullBackOff
* Pods READY = desired replicas

---

## Step 4 — Smoke Tests

Run basic tests:

* `/health`
* `/metrics`
* Auth login test
* Product listing
* Order create → Payment → Reservation
* Notification email simulation

Failure → rollback immediately.

---

# 4. Rollback Procedure

Rollback via:

### Option A — Previous Kubernetes ReplicaSet

```
kubectl rollout undo deploy/order-service
```

### Option B — Re-deploy previous version

```
kubectl set image deploy/order-service order-service:1.2.9
```

Rollback triggers:

* Error rate > 5%
* p99 latency > 2.5s
* DB connection saturation
* Kafka consumer errors

---

# 5. Pre-Deployment Checklist

* [ ] Code reviewed
* [ ] OpenAPI updated
* [ ] Database migrations tested on staging
* [ ] Backward-compatible changes
* [ ] Feature flags toggled safely
* [ ] Alerts quieted (deployment window)

---

# 6. Post-Deployment Validation

Validate:

* Grafana dashboard OK
* No spike in 500s
* Kafka consumer lag normal
* DB performance stable
* No increase in Redis TTL violations

Run synthetic tests:

* login
* create order
* payment sandbox
* search index

---

# 7. Deployment Windows

### Standard Deployment Windows:

* Weekdays: 09:00–17:00
* No Friday deploys after 15:00
* No weekend deploys unless emergency

### High-Risk Deployments

* Schema migrations
* Payment provider updates
* Kafka partition changes

→ require SRE approval.

---

# 8. Zero-Downtime Requirements

All services must support:

* Rolling updates
* Backward compatible API changes
* Graceful shutdown
* Idempotent event handling
* Readiness & liveness probes

---

# 9. When to Escalate

### Immediate escalation:

* Deployment failure during canary
* Payment errors detected
* Order creation stalled
* Kafka lag high
* 500s spike above threshold

Notify:

* On-call engineer
* SRE lead

---

# 10. End of Runbook

This is the authoritative deployment process for PolyShop.