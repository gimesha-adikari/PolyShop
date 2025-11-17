# Runbook: Feature Flags Management & Safe Releases

## Purpose

Defines how PolyShop engineers use, manage, and safely roll out new features using feature flags.

Applies to:

* Auth Service
* Product Service
* Inventory Service
* Order Service
* Payment Service
* Notification Service
* Search Service
* Gateway

PolyShop uses feature flags for:

* Gradual rollouts
* A/B testing
* Canary releases
* Kill switches
* Hotfix toggles
* Experimental features

---

# 1. Feature Flag Provider

PolyShop uses **OpenFeature** with pluggable backends:

### Local (Dev)

* `flags.json` file

### Staging / Production

* LaunchDarkly
  or
* Unleash self-hosted
  (Depending on environment)

Flags are read through a shared library:
`com.polyshop.common.feature.FeatureFlagClient`

---

# 2. Feature Flag Types

### Boolean Flags

Standard on/off switches.
Example:

* `enable-new-login-flow`
* `inventory.reservation.strict-mode`

### Variant Flags

Used for A/B tests or multi-behavior features.
Example:

* `payment.provider.selector = { stripe, paypal, mock }`

### Percentage Rollout Flags

Allow progressive rollout.
Example:

* Roll out feature to 5%, 20%, 50%, 100% of users.

### Kill Switches

Used to instantly disable:

* Payments
* Inventory locking
* Notification sending

---

# 3. Naming Conventions

Flags use a **qualified dot notation**:

```
<service>.<domain>.<feature>
```

Examples:

```
auth.login.new-ui
order.saga.enhanced-compensation
payment.provider.stripe-v3
inventory.reservation.strict-mode
notification.sms.new-template-engine
search.indexing.bulk-mode
```

---

# 4. Where Flags Are Used in Code

### Common usage pattern:

Java:

```java
if (featureFlagClient.isEnabled("inventory.reservation.strict-mode")) {
    // strict mode logic
}
```

Node:

```ts
if (flags.enabled("payment.provider.stripe-v3")) {
    processStripeV3();
}
```

Python:

```python
if flags.enabled("notification.sms.new-template-engine"):
    send_sms_v2()
```

Flags must NEVER be hardcoded.

---

# 5. Safe Release Workflow

## Step 1 — Add Flag

Add to LaunchDarkly/Unleash:

* Disabled by default
* Assign owners
* Add description + expiration date

## Step 2 — Deploy Code (Flag Off)

Deploy to production with flag OFF.
This ensures:

* No risk
* No user sees the feature

## Step 3 — Internal Enable (1%)

Enable ONLY for internal testers:

* developers
* QA
* staging user group

## Step 4 — Monitor

Check:

* Logs (Loki)
* Error rate
* Custom metrics
* Performance impact

## Step 5 — Gradual Rollout

Example:

```
1% → 5% → 20% → 50% → 100%
```

At each step:

* Monitor 5–15 minutes
* Check error/perf dashboards
* Stop immediately if anomalies occur

## Step 6 — Full Enable

Turn feature ON for all users.

## Step 7 — Cleanup

If successful:

* Remove old code paths
* Delete flag from provider within 30 days

---

# 6. Kill Switch Usage

Kill switches must be:

* Available for critical flows
* Cloud-controlled
* Active in <1 second propagation time (LaunchDarkly recommended)

Examples:

* `payment.disable-all`
* `inventory.disable-reservations`
* `order.saga.pause`
* `gateway.block-new-sessions`

When triggered:

* Immediately stop the service behavior
* Log reason
* Notify engineering automatically

---

# 7. A/B Testing Procedure

Used for UI/UX or algorithmic changes.

### Setup:

* Define experiment flag
* Percentage rollout (50/50)
* Track:

    * Conversion rate
    * Errors
    * Latency
    * User churn

### Data Collection

Analytics service aggregates metrics:

* `abtest.variant_a.conversion_total`
* `abtest.variant_b.conversion_total`

### Ending the Test

* Declare winner based on metrics
* Roll out winner to 100%
* Disable and delete flag

---

# 8. Incident Recovery Using Flags

Feature flags are critical for incident rollback.

### If a deployment breaks production:

1. Identify related feature flags
2. Disable flag (instant rollback)
3. Verify system recovery
4. Investigate bug
5. Patch & redeploy
6. Re-enable flag slowly

This is safer than redeploying or scaling down.

---

# 9. Expiration Policy

Every flag must have:

* Owner
* Creation timestamp
* Expiration date (max 60 days)

Expired flags:

* Trigger warnings in CI
* Auto-create Jira removal tasks

---

# 10. Security Considerations

* Flags MUST NOT expose sensitive data.
* Flags MUST NOT be accessible to frontend unless intended.
* Frontend-visible flags have separate namespace:

```
frontend.<page>.<feature>
```

* Avoid toggling security behaviors (authz/authn) via flags.

---

# 11. When to Escalate

Escalate if:

* Flags fail to update across services
* Kill switch lags in propagation
* Flag state inconsistent across pods
* Rollout causes spike in:

    * 500 errors
    * Login failures
    * Payment failures

---

# 12. End of Runbook

This is the authoritative guide for operating PolyShop feature flags.