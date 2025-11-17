# PolyShop Deployment – Development Environment

This document describes how to run the **entire PolyShop stack locally** using `docker-compose.dev.yml`.

## 1. File location

Place the compose file at:

```text
polyshop-root/
├── services/
├── infra/
│   └── docker/
│       └── compose/
│           └── docker-compose.dev.yml
└── docs/
    └── architecture/
```

And create a directory for database init scripts (optional, can be empty):

```text
infra/db/init/
```

Any `.sql` scripts placed there will run on container startup (for creating databases/users).

## 2. Networks and volumes

The compose file defines:

- Network:
  - `polyshop-net` – shared bridge network for all services.

- Volumes:
  - `postgres_data` – PostgreSQL data
  - `kafka_data` – Kafka logs
  - `zookeeper_data` – ZooKeeper data
  - `redis_data` – Redis persistence

These volumes keep data between container restarts in dev.

## 3. Core infrastructure

### PostgreSQL

- Image: `postgres:16-alpine`
- Port: `5432` (host) → `5432` (container)
- Default DB: `polyshop_dev`
- Default superuser: `postgres` / `postgres`
- Uses `infra/db/init/*.sql` for schema/database creation.

Each service uses its own database and user (created by SQL scripts), e.g.:

- `auth_db` with `auth_user/auth_password`
- `product_db` with `product_user/product_password`
- `inventory_db` with `inventory_user/inventory_password`
- `order_db` with `order_user/order_password`
- `analytics_db` with `analytics_user/analytics_password`

### Kafka + ZooKeeper

- `zookeeper`:
  - Port: `2181`

- `kafka`:
  - Internal broker: `kafka:9092` (used by services)
  - Host access: `localhost:9092` (exposed via `PLAINTEXT_HOST`)

### Redis

- Image: `redis:7-alpine`
- Port: `6379`
- Persistence enabled with `appendonly yes`.
- Used by inventory/idempotency/rate limiting if needed.

### Mailhog

- SMTP: `localhost:1025`
- Web UI: `http://localhost:8025`
- Used by Notification + Auth for development emails.

### OpenSearch

- HTTP: `http://localhost:9200`
- Single-node dev cluster with security disabled.
- Used by `search-service` for indexing products.

## 4. Application services (ports & URLs)

| Service              | Container name       | Internal URL                     | Host Port |
|----------------------|----------------------|----------------------------------|-----------|
| API Gateway          | `gateway`            | `http://gateway:8080`           | `8080`    |
| Auth Service         | `auth-service`       | `http://auth-service:8081`      | `8081`    |
| Product Service      | `product-service`    | `http://product-service:8082`   | `8082`    |
| Inventory Service    | `inventory-service`  | `http://inventory-service:8083` | `8083`    |
| Order Service        | `order-service`      | `http://order-service:8084`     | `8084`    |
| Payment Service      | `payment-service`    | `http://payment-service:8085`   | `8085`    |
| Notification Service | `notification-service`| `http://notification-service:8086` | `8086` |
| Search Service       | `search-service`     | `http://search-service:8087`    | `8087`    |
| Analytics Service    | `analytics-service`  | `http://analytics-service:8088` | `8088`    |

Gateway is the only service exposed to external clients in a realistic deployment. In dev, all services are also mapped to host ports for convenience.

## 5. Dev workflow

### 5.1 Build artifacts

For Java services (auth/product/inventory/order/search/gateway):

```bash
./gradlew :services:auth-service:bootJar
./gradlew :services:product-service:bootJar
# ... repeat for each service
```

Each jar should end up under:

- `services/<service-name>/build/libs/*.jar`

Compose mounts these directories into `/app` and runs `java -jar <service>.jar`.

For Node (payment-service):

```bash
cd services/payment-service
npm install
npm run build # if you have a build step; otherwise adjust Dockerfile
```

For Python (notification/analytics):

- Compose uses Dockerfiles under each service; you can mount code for live reload later if needed.

### 5.2 Start the stack

From repository root:

```bash
cd infra/docker/compose
docker compose -f docker-compose.dev.yml up -d
```

To see logs:

```bash
docker compose -f docker-compose.dev.yml logs -f
```

To stop:

```bash
docker compose -f docker-compose.dev.yml down
```

To reset data (DB/Kafka/Redis):

```bash
docker compose -f docker-compose.dev.yml down -v
```

## 6. Production vs development notes

- In **development**:
  - All services expose ports on localhost for easy debugging.
  - Databases, Kafka, Redis, etc. share the same Docker network and are single-node.
  - Credentials and URLs are simple and stored directly in `docker-compose.dev.yml` or `.env`.
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is acceptable for quick schema evolution.

- In **production** (later phases):
  - Replace `docker-compose.dev.yml` with:
    - `docker-compose.k8s.yml` (for local K8s bootstrap) or
    - Native Kubernetes manifests / Helm charts.
  - Use **separate managed services** for Postgres, Kafka, Redis, and OpenSearch.
  - Use **Vault/Secrets Manager** instead of plaintext passwords.
  - Move to **multi-replica** services, with horizontal pod autoscaling and proper resource limits.
  - Disable debug endpoints and use strong CORS/ingress rules at the gateway.

This dev compose file is intentionally simple but **covers all dependencies** needed to start coding services against real Postgres, Kafka, Redis, and OpenSearch locally.
