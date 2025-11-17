# ADR-0002: Database Strategy for PolyShop

## Status
Accepted

## Date
2025-11-17

## Context
PolyShop includes multiple microservices with distinct domains:
- Auth
- Product
- Inventory
- Order
- Payment
- Notification
- Analytics

Each domain has unique read/write patterns, access rules, and scaling requirements. A shared database across services would create tight coupling, unsafe cross-service access, and unsafe transaction boundaries.

## Decision
Adopt a **“Database-per-Service”** architecture:
- Each service owns **its own PostgreSQL database**.
- No service is allowed to read/write another service’s database directly.
- Cross-service communication is achieved through:
    - Synchronous REST calls (gateway → internal)
    - Asynchronous Kafka events
- Shared schemas are defined only in `libs/common-*`, not in shared DB tables.

## Data Stores Chosen

| Service        | Store Type             | Purpose |
|----------------|-------------------------|---------|
| Auth           | PostgreSQL              | Users, roles, sessions |
| Product        | PostgreSQL              | Products, variants, categories |
| Inventory      | PostgreSQL              | Stock, reservations, movements |
| Order          | PostgreSQL              | Orders, order items |
| Payment        | PostgreSQL              | Payments, refunds |
| Notification   | PostgreSQL              | Templates, message logs |
| Search Service | OpenSearch / Elasticsearch | Search indexing |
| Analytics      | PostgreSQL + Parquet (future) | Aggregated metrics, fact tables |

## Consequences

### Positive
- Strong isolation: each service controls its own schema
- Zero cross-service accidental coupling
- Easier backups and migrations
- Improved scalability (per store)

### Negative
- Harder cross-service querying (design must rely on events)
- More databases to manage in production

## Alternatives Considered

### 1. Single Shared Database
Rejected due to:
- Tight coupling
- Hard migrations
- Impossible proper domain boundaries
- Performance bottleneck

### 2. Schema-per-Service (same DB server)
Rejected for production:
- Still couples services to same server
- No isolation on resource usage

## Final Decision
Each microservice manages its own PostgreSQL instance (or dedicated database on the same cluster for dev). Async replication to analytics is enabled via CDC in the future.
