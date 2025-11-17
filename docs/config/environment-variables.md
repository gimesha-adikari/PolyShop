# **PolyShop — Environment Variables Specification**

This document defines **all required and optional environment variables** for every microservice, including common conventions, secrets, and `.env.example` files.

---

# **1. Global Environment Variable Rules**

## **1.1 Naming**

All variables must follow:

```
UPPER_SNAKE_CASE
```

Service-scoped variables use prefixing:

```
AUTH_*
PRODUCT_*
INVENTORY_*
ORDER_*
PAYMENT_*
SEARCH_*
NOTIFICATION_*
GATEWAY_*
```

---

## **1.2 Required vs Optional**

* **Required** → service must refuse to start if missing
* **Optional** → has a documented default

---

## **1.3 Secrets & Sensitive Fields**

Values containing:

* passwords
* tokens
* private keys
* connection strings
* API keys

must **never** be committed into git or `.env.example`.

---

## **1.4 Shared Variables Across All Services**

| Variable                  | Required | Description                                   |
| ------------------------- | -------- | --------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`  | optional | Usually `dev`, `prod`                         |
| `LOG_LEVEL`               | optional | `INFO`, `DEBUG`, etc.                         |
| `PORT`                    | optional | HTTP port (default varies per service)        |
| `JWT_PUBLIC_KEY`          | required | PEM encoded public key shared by all services |
| `JWT_ISSUER`              | required | Token issuer, e.g., `polyshop-auth`           |
| `REDIS_HOST`              | required | Redis hostname                                |
| `REDIS_PORT`              | optional | Defaults to `6379`                            |
| `KAFKA_BOOTSTRAP_SERVERS` | required | Kafka brokers                                 |
| `DB_SSL`                  | optional | On/off for production DB security             |

---

# **2. Service-Specific Variables**

---

# **2.1 Auth Service**

### **Purpose**

Authentication, refresh tokens, sessions, password resets, email verification.

### **Variables**

| Variable                  | Required | Description                 |
| ------------------------- | -------- | --------------------------- |
| `AUTH_DB_URL`             | required | JDBC URL for PostgreSQL     |
| `AUTH_DB_USERNAME`        | required | DB user                     |
| `AUTH_DB_PASSWORD`        | required | DB password                 |
| `AUTH_JWT_PRIVATE_KEY`    | required | Private signing key         |
| `AUTH_JWT_EXPIRES_IN`     | required | Access token TTL (seconds)  |
| `AUTH_REFRESH_EXPIRES_IN` | required | Refresh token TTL (seconds) |
| `AUTH_RATE_LIMIT_LOGIN`   | optional | Max attempts per minute     |
| `AUTH_EMAIL_FROM`         | optional | Default email sender        |
| `AUTH_NOTIFICATION_URL`   | optional | URL to Notification Service |

---

# **2.2 Product Service**

| Variable              | Required | Description             |
| --------------------- | -------- | ----------------------- |
| `PRODUCT_DB_URL`      | required | JDBC connection         |
| `PRODUCT_DB_USERNAME` | required | DB user                 |
| `PRODUCT_DB_PASSWORD` | required | DB password             |
| `MEDIA_BUCKET`        | required | Storage bucket name     |
| `MEDIA_BASE_URL`      | required | Public image URL prefix |
| `SEARCH_INDEX_URL`    | optional | For forced re-indexing  |

---

# **2.3 Inventory Service**

| Variable                                 | Required | Description                       |
| ---------------------------------------- | -------- | --------------------------------- |
| `INVENTORY_DB_URL`                       | required | PostgreSQL URL                    |
| `INVENTORY_DB_USERNAME`                  | required | DB user                           |
| `INVENTORY_DB_PASSWORD`                  | required | DB password                       |
| `INVENTORY_RESERVATION_TTL`              | optional | Default reservation TTL (seconds) |
| `INVENTORY_RESERVATION_CLEANUP_INTERVAL` | optional | Scheduler period                  |
| `INVENTORY_KAFKA_TOPIC_RESERVATIONS`     | required | Topic name                        |

---

# **2.4 Order Service**

| Variable                   | Required | Description         |
| -------------------------- | -------- | ------------------- |
| `ORDER_DB_URL`             | required | JDBC URL            |
| `ORDER_DB_USERNAME`        | required | DB user             |
| `ORDER_DB_PASSWORD`        | required | DB password         |
| `ORDER_SAGA_TIMEOUT`       | required | Saga timeout (ms)   |
| `ORDER_KAFKA_TOPIC_EVENTS` | required | Topic name          |
| `INVENTORY_SERVICE_URL`    | required | Sync actions (rare) |

---

# **2.5 Payment Service**

| Variable               | Required | Description              |
| ---------------------- | -------- | ------------------------ |
| `PAYMENT_DB_URL`       | required | JDBC                     |
| `PAYMENT_DB_USERNAME`  | required | DB user                  |
| `PAYMENT_DB_PASSWORD`  | required | DB password              |
| `STRIPE_SECRET_KEY`    | optional | Stripe integration       |
| `PAYPAL_CLIENT_ID`     | optional | PayPal integration       |
| `PAYPAL_CLIENT_SECRET` | optional | PayPal integration       |
| `WEBHOOK_SECRET`       | required | Verify provider webhooks |

---

# **2.6 Search Service**

| Variable             | Required | Description                  |
| -------------------- | -------- | ---------------------------- |
| `SEARCH_ELASTIC_URL` | required | Elasticsearch/OpenSearch URL |
| `SEARCH_INDEX_NAME`  | required | Product index                |
| `SEARCH_USERNAME`    | optional | If cluster is secured        |
| `SEARCH_PASSWORD`    | optional | If cluster is secured        |

---

# **2.7 Notification Service**

| Variable                 | Required | Description       |
| ------------------------ | -------- | ----------------- |
| `NOTIF_SMTP_HOST`        | required | SMTP server       |
| `NOTIF_SMTP_PORT`        | optional | Defaults to `587` |
| `NOTIF_SMTP_USERNAME`    | required | SMTP auth         |
| `NOTIF_SMTP_PASSWORD`    | required | SMTP auth         |
| `NOTIF_SMS_PROVIDER_KEY` | optional | For SMS sending   |
| `NOTIF_DB_URL`           | required | For templates     |
| `NOTIF_DB_USERNAME`      | required | DB user           |
| `NOTIF_DB_PASSWORD`      | required | DB password       |

---

# **2.8 Gateway**

| Variable                   | Required | Description     |
| -------------------------- | -------- | --------------- |
| `GATEWAY_PORT`             | optional | Defaults `8080` |
| `GATEWAY_RATE_LIMIT`       | optional | Requests/second |
| `SERVICE_AUTH_URL`         | required | Auth service    |
| `SERVICE_PRODUCT_URL`      | required | Product         |
| `SERVICE_ORDER_URL`        | required | Order           |
| `SERVICE_PAYMENT_URL`      | required | Payment         |
| `SERVICE_SEARCH_URL`       | required | Search          |
| `SERVICE_NOTIFICATION_URL` | required | Notification    |

---

# **3. Combined `.env.example`**

Save this in root:

```
/.env.example
```

```
### GLOBAL ###
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=INFO

REDIS_HOST=redis
REDIS_PORT=6379

KAFKA_BOOTSTRAP_SERVERS=kafka:9092
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----...

JWT_ISSUER=polyshop-auth


### AUTH SERVICE ###
AUTH_DB_URL=jdbc:postgresql://auth-db:5432/auth
AUTH_DB_USERNAME=auth_user
AUTH_DB_PASSWORD=secret
AUTH_JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----...
AUTH_JWT_EXPIRES_IN=900
AUTH_REFRESH_EXPIRES_IN=2592000
AUTH_RATE_LIMIT_LOGIN=5
AUTH_EMAIL_FROM=noreply@polyshop.com
AUTH_NOTIFICATION_URL=http://notification:8086


### PRODUCT SERVICE ###
PRODUCT_DB_URL=jdbc:postgresql://product-db:5432/product
PRODUCT_DB_USERNAME=product_user
PRODUCT_DB_PASSWORD=secret
MEDIA_BUCKET=product-images
MEDIA_BASE_URL=http://localhost:9000/images/
SEARCH_INDEX_URL=http://search:8087


### INVENTORY SERVICE ###
INVENTORY_DB_URL=jdbc:postgresql://inventory-db:5432/inventory
INVENTORY_DB_USERNAME=inventory_user
INVENTORY_DB_PASSWORD=secret
INVENTORY_RESERVATION_TTL=300
INVENTORY_KAFKA_TOPIC_RESERVATIONS=inventory.reservations


### ORDER SERVICE ###
ORDER_DB_URL=jdbc:postgresql://order-db:5432/orders
ORDER_DB_USERNAME=order_user
ORDER_DB_PASSWORD=secret
ORDER_SAGA_TIMEOUT=30000
ORDER_KAFKA_TOPIC_EVENTS=order.events
INVENTORY_SERVICE_URL=http://inventory:8083


### PAYMENT SERVICE ###
PAYMENT_DB_URL=jdbc:postgresql://payment-db:5432/payments
PAYMENT_DB_USERNAME=payment_user
PAYMENT_DB_PASSWORD=secret
STRIPE_SECRET_KEY=
PAYPAL_CLIENT_ID=
PAYPAL_CLIENT_SECRET=
WEBHOOK_SECRET= replace_me


### SEARCH SERVICE ###
SEARCH_ELASTIC_URL=http://elasticsearch:9200
SEARCH_INDEX_NAME=products
SEARCH_USERNAME=
SEARCH_PASSWORD=


### NOTIFICATION SERVICE ###
NOTIF_SMTP_HOST=smtp.mail.com
NOTIF_SMTP_PORT=587
NOTIF_SMTP_USERNAME=user
NOTIF_SMTP_PASSWORD=secret
NOTIF_SMS_PROVIDER_KEY=
NOTIF_DB_URL=jdbc:postgresql://notif-db:5432/notif
NOTIF_DB_USERNAME=notif_user
NOTIF_DB_PASSWORD=secret


### GATEWAY ###
GATEWAY_PORT=8080
GATEWAY_RATE_LIMIT=50
SERVICE_AUTH_URL=http://auth:8081
SERVICE_PRODUCT_URL=http://product:8082
SERVICE_ORDER_URL=http://order:8084
SERVICE_PAYMENT_URL=http://payment:8085
SERVICE_SEARCH_URL=http://search:8087
SERVICE_NOTIFICATION_URL=http://notification:8086
```

