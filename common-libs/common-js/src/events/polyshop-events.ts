export interface EventEnvelope<TPayload = unknown> {
    id: string;
    type: string;
    source: string;
    payload: TPayload;
    correlationId?: string;
    causationId?: string;
    occurredAt: string;
    version: number;
}

// AUTH EVENTS

export interface UserRegisteredPayload {
    userId: string;
    email: string;
    fullName?: string;
    registeredAt: string;
}

export interface UserEmailVerifiedPayload {
    userId: string;
    email: string;
    verifiedAt: string;
}

// ORDER EVENTS

export interface OrderCreatedItem {
    productId: string;
    variantId?: string | null;
    quantity: number;
}

export interface OrderCreatedPayload {
    orderId: string;
    userId: string;
    items: OrderCreatedItem[];
    totalAmount: number;
    currency: string;
    createdAt: string;
}

export interface OrderStatusChangedPayload {
    orderId: string;
    previousStatus: string;
    newStatus: string;
    changedAt: string;
    reason?: string;
}

// PAYMENT EVENTS

export interface PaymentCreatedPayload {
    paymentId: string;
    orderId: string;
    amount: number;
    currency: string;
    provider: string;
    status: string;
    createdAt: string;
}

export interface PaymentStatusChangedPayload {
    paymentId: string;
    orderId: string;
    previousStatus: string;
    newStatus: string;
    changedAt: string;
    providerRef?: string;
}

// INVENTORY EVENTS

export interface InventoryReservedPayload {
    reservationId: string;
    orderId: string;
    productId: string;
    variantId?: string | null;
    quantity: number;
    reservedAt: string;
    expiresAt?: string | null;
}

export interface InventoryReservationReleasedPayload {
    reservationId: string;
    orderId: string;
    productId: string;
    variantId?: string | null;
    quantity: number;
    releasedAt: string;
    reason?: string;
}

export interface InventoryReservationConfirmedPayload {
    reservationId: string;
    orderId: string;
    productId: string;
    variantId?: string | null;
    quantity: number;
    confirmedAt: string;
}

// NOTIFICATION EVENTS

export interface NotificationEmailRequestedPayload {
    notificationId: string;
    templateName: string;
    to: string;
    variables?: Record<string, unknown>;
    requestedAt: string;
}

export interface NotificationSmsRequestedPayload {
    notificationId: string;
    templateName: string;
    to: string;
    variables?: Record<string, unknown>;
    requestedAt: string;
}
