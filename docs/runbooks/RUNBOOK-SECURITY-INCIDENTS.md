# Runbook: Security Incident Response

## Purpose

Defines how PolyShop engineers detect, respond to, contain, and recover from security incidents across all environments.

Incidents include:

* Credential leaks
* Unauthorized access attempts
* Suspicious API activity
* DDoS or traffic spikes
* Compromised tokens/sessions
* Data exposure or corruption
* Dependency vulnerabilities

This runbook applies to all PolyShop services and infrastructure.

---

# 1. Security Monitoring Sources

PolyShop continuously monitors:

### 1.1 Logs (Loki)

* Failed login attempts
* Suspicious IPs
* Unknown `User-Agent` patterns
* Repeated 401/403 responses
* Token signature failures

### 1.2 Metrics (Prometheus)

* Rate limiting triggers
* Spike in failed login events
* Abnormal traffic burst on gateway

### 1.3 Alerts

* GitHub secret scanning
* Snyk/D dependabot alerts
* Cloud provider IAM events
* K8s audit logs

### 1.4 External Scanners

* OWASP ZAP
* Burp Suite (manual penetration test)

---

# 2. Incident Severity Levels

### **Severity 1 – Critical**

* Active data breach
* Unauthorized access confirmed
* Compromised JWT signing key
* Database dump stolen
* Ransomware attack

Action: Notify CTO + SRE immediately. Full shutdown if needed.

---

### **Severity 2 – High**

* Admin account compromise
* Payment provider API key leak
* Service-to-service token exposed
* High-rate suspicious traffic

Action: Contain affected service within 15 minutes.

---

### **Severity 3 – Medium**

* Rate limit abuse
* Repeated failed logins
* Misconfigured CORS / open endpoints

Action: Patch within 24 hours.

---

### **Severity 4 – Low**

* Outdated dependencies
* Minor vulnerability in non-critical library

Action: Fix in normal development cycle.

---

# 3. Incident Response Lifecycle

PolyShop follows a strict incident response lifecycle:

## Step 1 — **Detection**

Triggered by:

* Alerts
* Developer report
* Customer complaint
* Monitoring dashboards

Document:

* Timestamp
* Source of detection
* Initial suspicion

---

## Step 2 — **Containment**

Immediate actions:

* Disable compromised tokens
* Block offending IPs
* Switch JWT signing key (rotate)
* Quarantine affected services
* Disable suspicious user accounts
* Bring Gateway into “restricted mode”

Containment must occur **within minutes**.

---

## Step 3 — **Eradication**

After containment:

* Patch vulnerability
* Fix misconfigured firewall rules
* Update dependencies
* Revoke leaked secrets
* Clean compromised containers
* Rotate DB passwords
* Rotate Stripe/PayPal API keys

---

## Step 4 — **Recovery**

Restore services:

* Deploy patched version
* Re-enable traffic routing
* Validate normal behavior
* Check logs for recurrence

For S1/S2 incidents:

* Run full integration tests
* Confirm DB integrity

---

## Step 5 — **Post-Incident Review**

Within 24 hours:

* Write incident report
* Root cause analysis
* Timeline of events
* Impact measurement
* Prevention steps
* ADR update if architecture change needed

---

# 4. Common Incident Scenarios

## 4.1 Suspicious Login Activity

**Signs:**

* Repeated login failures
* Multiple IPs trying same accounts

**Actions:**

1. Trigger rate limiting policy
2. Block offending IP ranges
3. Force password reset for affected users
4. Notify security team

---

## 4.2 JWT Signing Key Compromise

**Actions:**

1. **Rotate signing key immediately**
2. Invalidate all active access tokens
3. Force refresh token reset
4. Regenerate tokens for all active users
5. Investigate source of leak
6. Patch and redeploy affected services

---

## 4.3 Database Credential Leak

**Actions:**

1. Rotate DB password(s)
2. Rollout restart all services
3. Check DB logs for unauthorized reads
4. Validate data integrity
5. Restore from backup if needed

---

## 4.4 Payment Provider API Key Leak

**Actions:**

1. Rotate Stripe / PayPal API keys
2. Block charges until validation passes
3. Scan logs for suspicious usage
4. Alert payment provider
5. Deploy updated secrets

---

## 4.5 DDoS Attack

Detected by:

* Sudden surge in traffic
* Gateway CPU spikes
* Many 429 rate-limit events

**Actions:**

1. Enable high-security mode at gateway
2. Apply IP-based throttling
3. Enable Cloudflare “under attack” mode
4. Scale gateway replicas
5. Block known hostile ASN ranges

---

## 4.6 Vulnerable Dependency Alert

Triggered by:

* Snyk
* Dependabot
* GitHub code scanning

**Actions:**

1. Patch dependency
2. Rebuild service
3. Re-deploy to staging
4. Re-run security tests
5. Promote to production

---

# 5. Secrets Rotation Policy

All secrets must be rotated:

* Every 60 days for dev
* Every 30 days for production
* Immediately after suspected compromise

Secrets include:

* JWT signing keys
* DB passwords
* Kafka credentials
* Stripe & PayPal keys
* Email/SMS provider keys
* OAuth app secrets
* Redis passwords

Use Vault for automated rotation where possible.

---

# 6. Communication Protocol

### For **Severity 1 (Critical)**:

* Notify CTO, SRE Lead, Backend Lead
* Declare incident in #incident-response channel
* Update status page every 15 minutes

### For **Severity 2 (High)**:

* Notify SRE + Infra team
* Update status page every 30–60 minutes

### For **Severity 3–4**:

* Create Jira ticket
* Fix during next sprint

---

# 7. Evidence Collection Rules

Collect:

* Logs (last 48 hours)
* Identity provider logs
* Gateway logs
* DB activity logs
* Request/response dumps (anonymized)

Preserve:

* Container snapshots
* Broken deployment manifests

Never collect:

* User passwords
* Full credit card numbers
* Sensitive PII

---

# 8. When to Escalate

Escalate **immediately** if:

* Customer data is exposed
* Payments are affected
* Admin accounts compromised
* DB or Redis tampered
* Suspicious production access

---

# 9. End of Runbook

This is the authoritative security-incident response guide for PolyShop.
