# ADR-0001: PolyShop Overall Architecture Choice

## Status
Accepted

## Date
2025-11-17

## Context
PolyShop requires a scalable, modular, easily deployable architecture that supports:
- Independent microservices
- Polyglot service implementation (Java, Node.js, Python)
- Event-driven workflows (Kafka)
- Container orchestration (Docker, Kubernetes)
- A unified API surface through an API Gateway
- Strong observability, QA automation, and CI/CD

The team also needs the ability to scale services independently (e.g., Search needs more CPU, Notification needs more workers).

## Decision
PolyShop is implemented using a **microservices architecture** backed by:
1. **Spring Boot** for core domain services (Auth, Product, Order, Payment)
2. **Node.js** for Payment provider integrations
3. **Python** for Analytics & Notification processing
4. **Kafka** for event-driven communication
5. **Kubernetes** for orchestration
6. **NGINX-based API Gateway** as the public API entry point
7. **PostgreSQL** as the main transactional database for each service
8. **Redis** for caching and temporary state (tokens, ratelimiting, reservations)
9. **ELK + Prometheus/Grafana** for observability

## Consequences
### Positive
- Independent scaling, deployment, and fault isolation
- Easy integration of different technologies per service needs
- Strong support for async workflows and sagas
- Clear domain boundaries following DDD principles

### Negative
- Increase in operational complexity
- Requires proper observability and tracing
- Higher cost compared to a monolithic approach

## Alternatives Considered
### 1. Monolithic Application
Rejected due to:
- Lack of independent scaling
- More complexity during long-term maintenance
- Harder to introduce polyglot service patterns

### 2. Modular Monolith
Rejected due to:
- Still limited boundary separation
- Does not fit well with event-driven workflow requirements

## Decision Outcome
Microservices architecture is adopted as the long-term strategic solution for PolyShop.
