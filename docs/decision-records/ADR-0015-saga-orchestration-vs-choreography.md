# ADR-0015: Saga Orchestration vs Choreography

## Status
Accepted

## Date
2025-02-20

## Context
PolyShop uses distributed transactions across several microservices:
- Order Service
- Inventory Service
- Payment Service
- Notification Service

A single order triggers multiple steps:
1. Create order
2. Reserve stock
3. Process payment
4. Confirm stock
5. Mark order as paid
6. Trigger email/sms

We need a reliable Saga design with clear compensation logic.

## Options Considered
### Option A — Choreographed Saga (event-driven)
- Each service reacts to events.
- No central coordinator.
- High decoupling.

**Pros**
- Simple to extend.
- Loose coupling.
- Natural fit for Kafka.

**Cons**
- Harder to debug.
- Complex event chains.
- Risk of "unpredictable flow".

### Option B — Orchestrated Saga (central orchestrator)
- One service coordinates all steps.
- Other services expose commands (HTTP/async).

**Pros**
- Easier to reason about.
- Clear step-by-step state machine.
- Centralized error handling.
- Clean compensation logic.

**Cons**
- Slightly more coupling.
- Orchestrator must be highly available.

## Decision
Use **Hybrid Saga**:
- **Order Service = Orchestrator**
- Inventory/Payment act as **participants**
- All communication event-driven through Kafka
- Orchestrator drives the workflow:
    - ORDER_CREATED → STOCK_RESERVATION_REQUEST → PAYMENT_REQUEST → ...
- Participants emit events back.

This gives:
- Clear state machine
- Audit-friendly flow
- Distributed but controlled

## Consequences
### Positive
- Simplifies debugging
- Predictable workflow
- Easy compensation (release stock, cancel payment, etc.)
- Order service owns full lifecycle states

### Negative
- Order service becomes a critical orchestrator
- Slight increase in central logic
- Must maintain idempotency on all steps

## Future Considerations
- Introduce Saga Timeouts per step
- Add retry policies per service
- Use outbox table for reliability  
