# PolyShop – System Configuration

## Service Ports

| Service              | Port |
|----------------------|------|
| gateway              | 8080 |
| auth-service         | 8081 |
| product-service      | 8082 |
| inventory-service    | 8083 |
| order-service        | 8084 |
| payment-service      | 8085 |
| notification-service | 8086 |
| search-service       | 8087 |
| analytics-service    | 8088 |

These ports are for local development and Docker Compose. Kubernetes ingress will map external ports differently.

---

## Databases (PostgreSQL per service)

| Service              | Database name    |
|----------------------|------------------|
| auth-service         | auth_db          |
| product-service      | product_db       |
| inventory-service    | inventory_db     |
| order-service        | order_db         |
| payment-service      | payment_db       |
| notification-service | notification_db  |
| search-service       | search_db        |
| analytics-service    | analytics_db     |

Each database is owned exclusively by its service. No cross-service DB access.

---

## Local Infrastructure (planned for docker-compose.dev.yml)

- PostgreSQL (one instance with multiple DBs or multiple instances).
- Redis:
  - Caching.
  - Rate limiting.
- Kafka + Zookeeper:
  - Event-driven communication.
- Optional:
  - MinIO/S3-compatible storage for product images.
  - Elasticsearch/OpenSearch for search-service.

---

## Environment Strategy

- Each service reads configuration from:
  - `application.yml` (Java) / `.env` (Node/Python).
  - Environment variables for secrets and overrides.
- Local development uses `.env` files (not committed).
- Production uses:
  - Kubernetes Secrets for sensitive values.
  - ConfigMaps or external configuration service for non-secret config.

Example env variable naming conventions:

- `AUTH_DB_URL`, `AUTH_JWT_SECRET`
- `PRODUCT_DB_URL`
- `ORDER_DB_URL`
- `KAFKA_BROKER_URL`
- `REDIS_URL`
