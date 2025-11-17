# Runbook: CI/CD Pipeline Operations

## Purpose

Defines how PolyShop’s CI/CD pipelines are structured, monitored, debugged, and recovered when failures occur.

This runbook applies to:

* Backend microservices (Auth, Product, Inventory, Order, Payment, Notification, Search)
* API Gateway
* Shared libraries
* Infrastructure manifests
* QA test suites (Newman, Pact, k6)

---

# 1. CI/CD Overview

PolyShop uses:

| Component         | Tool                                       |
| ----------------- | ------------------------------------------ |
| CI                | GitHub Actions                             |
| Artifact Registry | GitHub Packages / Docker Hub               |
| CD                | ArgoCD (production) / docker-compose (dev) |
| Quality Gates     | SonarQube                                  |
| Security          | Dependabot + Snyk                          |
| Tests             | JUnit, Jest, Newman, Pact, k6              |

Pipelines run for each PR and main branch push.

---

# 2. Pipeline Stages

### Stage 1 — Lint & Static Analysis

* Java → Checkstyle + SpotBugs
* JS/TS → ESLint
* Python → flake8
* Security scanning → Snyk
* Dependency vulnerabilities → GitHub Dependabot

### Stage 2 — Build & Test

Per language:

* Java → `./gradlew build test`
* Node → `npm install && npm test`
* Python → `pytest`

### Stage 3 — QA Suites

Executed after unit tests:

* Newman API tests
* Pact contract tests
* k6 basic load test

### Stage 4 — Build Docker Images

All services build images:

* `auth-service:latest`
* `order-service:latest`
* `payment-service:latest`
* etc.

### Stage 5 — Push to Registry

* Cloud registry (prod)
* Local registry (dev)

### Stage 6 — Deploy

* Dev environment → docker-compose
* Staging/Production → ArgoCD GitOps sync

---

# 3. Pipeline Failure Diagnosis

## 3.1 Linting Stage Fails

Possible reasons:

* Style violations
* ESLint errors
* TypeScript type failures

Fix locally:

```
npm run lint
./gradlew check
```

---

## 3.2 Unit Tests Fail

Common causes:

* Incorrect mocks
* Missing Kafka or DB test containers
* Regression in shared libraries

Use local troubleshooting:

```
docker compose up test-db
./gradlew test --info
```

---

## 3.3 Contract Tests Fail (Pact)

Cause:

* Service changed API response shape
* OpenAPI spec mismatch
* Missing provider verification

Required action:

* Regenerate pact stubs
* Sync contracts across repos

---

## 3.4 Newman API Tests Fail

Likely:

* Gateway routing incorrect
* Service not started
* Wrong environment variables

Check logs of:

* Gateway
* Target service
* Newman output

---

## 3.5 Docker Build Failure

Common reasons:

* Incorrect Java version
* Missing dependency in Node service
* Wrong COPY paths
* Uncached dependency layer

Debug:

```
docker build --no-cache .
```

---

## 3.6 Deployment Failure (ArgoCD)

Possible causes:

* Kubernetes manifest invalid
* Wrong image tag
* CrashLoopBackOff in pods

Check:

```
kubectl logs <pod>
kubectl describe pod <pod>
```

---

# 4. Rollback Procedures

## Automated rollback

ArgoCD supports:

* Rollback to last successful deployment
* Auto-sync off → manual review

## Manual rollback:

1. Identify last healthy image:

```
kubectl rollout history deploy/product-service
```

2. Roll back:

```
kubectl rollout undo deploy/product-service --to-revision=<id>
```

## Rollback Gateway only:

* Route traffic to maintenance mode
* Disable new order creation
* Keep read APIs alive

---

# 5. Environment Promotion Flow

### Dev → Staging

Triggered on merge to main:

* All tests must pass
* Version bump
* New images pushed

### Staging → Production

Triggered manually:

* SRE approval
* ArgoCD sync
* Canary rollout optional

---

# 6. CI/CD Secrets Management

Stored in:

* GitHub Actions Secrets
* Kubernetes Secrets
* ArgoCD Vault Plugin (prod)

Secrets include:

* JWT signing keys
* DB passwords
* Kafka credentials
* Stripe/Paypal keys
* Email/SMS provider keys

Never commit secrets.

---

# 7. Canary Deployment Procedure

Services can be released in 3 modes:

1. **Shadow Traffic** (observe only)
2. **10% traffic**
3. **100% traffic**

Steps:

1. Deploy v2 alongside v1
2. Route partial traffic via Gateway rules
3. Monitor:

    * latency
    * 4xx/5xx
    * payment success rates
4. Promote to full release

Rollback if errors exceed threshold.

---

# 8. Observability During Deployments

Check:

* p50 / p90 / p99 latency
* CPU & memory
* DB connections
* Kafka lag
* Errors logged per minute

If anything spikes:

* Pause rollout
* Scale pods
* Revert deployment

---

# 9. When to Escalate

Immediately escalate if:

* Deployment causes order failures
* Payment provider errors increase
* Login/auth failing
* Consumer lag > 20k
* DB connection saturation

Escalate to:

* SRE lead
* Backend lead
* DevOps engineer

---

# 10. End of Runbook

This is the authoritative CI/CD operations runbook for PolyShop.