# PolyShop – Data Transfer Objects (DTOs)

This document describes shared DTOs used across services.
Language-specific representations will live in:

- `libs/common-java/`
- `libs/common-js/`
- `libs/common-py/`

---

## UserDTO (Auth Service)

Fields:
- `id: string`
- `email: string`
- `name: string`
- `roles: string[]`

Used in:
- auth-service responses.
- Gateway user context.
- order-service to tie orders to users.

---

## ProductDTO (Product Service)

Fields:
- `id: string`
- `name: string`
- `description: string`
- `price: number`
- `currency: string`
- `categoryId: string`
- `brand: string`
- `images: string[]` (URLs)
- `active: boolean`

Used in:
- Client product listing/detail.
- order-service for line items.
- search-service indexing.

---

## InventoryStatusDTO (Inventory Service)

Fields:
- `productId: string`
- `available: number`
- `reserved: number`

Used in:
- order-service checkout validation.
- product detail pages (optional).

---

## OrderItemDTO

Fields:
- `productId: string`
- `quantity: number`
- `unitPrice: number`
- `totalPrice: number`

---

## OrderDTO (Order Service)

Fields:
- `id: string`
- `userId: string`
- `items: OrderItemDTO[]`
- `totalAmount: number`
- `currency: string`
- `status: string` (CREATED, PENDING_PAYMENT, PAID, CANCELLED, FULFILLED)
- `createdAt: string` (ISO datetime)
- `updatedAt: string` (ISO datetime)

---

## PaymentDTO (Payment Service)

Fields:
- `id: string`
- `orderId: string`
- `amount: number`
- `currency: string`
- `status: string` (INITIATED, SUCCESS, FAILED)
- `provider: string` (e.g., "stripe")
- `providerRef: string` (gateway-specific ID)
- `createdAt: string`
- `updatedAt: string`

---

## NotificationRequestDTO (Notification Service)

Fields:
- `type: string` ("EMAIL" or "SMS")
- `to: string`
- `template: string`
- `variables: object` (key/value for template placeholders)

---

## Event Payloads (Kafka)

### product.events

`product.created` / `product.updated`:
- `id`
- `name`
- `price`
- `categoryId`
- `brand`
- `active`

`product.deleted`:
- `id`

---

### order.events

`order.created`:
- `id`
- `userId`
- `totalAmount`
- `status`
- `createdAt`

`order.paid`:
- `id`
- `paymentId`
- `totalAmount`
- `paidAt`

`order.cancelled`:
- `id`
- `reason`

---

### payment.events

`payment.initiated`:
- `id`
- `orderId`
- `amount`
- `provider`

`payment.success`:
- `id`
- `orderId`
- `amount`
- `provider`
- `paidAt`

`payment.failed`:
- `id`
- `orderId`
- `amount`
- `provider`
- `reason`
