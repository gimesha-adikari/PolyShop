export const EventTypes = {
    ORDER_CREATED: "order.created",
    ORDER_CANCELLED: "order.cancelled",
    PAYMENT_SUCCEEDED: "payment.succeeded",
    PAYMENT_FAILED: "payment.failed",
    STOCK_RESERVED: "stock.reserved",
    STOCK_RESERVATION_FAILED: "stock.reservation_failed"
} as const;

export type EventType = (typeof EventTypes)[keyof typeof EventTypes];

export interface EventEnvelope<T> {
    eventId: string;
    eventType: EventType;
    aggregateType: string;
    aggregateId: string;
    correlationId?: string;
    causationId?: string;
    occurredAt: string;
    payload: T;
    metadata?: Record<string, unknown>;
}
