# PolyShop API Schemas

This module centralizes all API contracts for PolyShop.

## Layout

- `openapi/`
    - `auth-service.yaml`
    - `product-service.yaml`
    - `inventory-service.yaml`
    - `order-service.yaml`
    - `payment-service.yaml`
    - `notification-service.yaml`
    - `search-service.yaml`
    - `analytics-service.yaml`
    - `gateway.yaml`
    - `common.yaml`

These files mirror the ones under `docs/api/openapi-specs/` and can be used
for:

- client/server code generation
- schema validation
- API documentation tooling.

In future, you can add:

- `protobuf/` — protobuf definitions for async event contracts
- `json-schemas/` — derived JSON Schema documents.
