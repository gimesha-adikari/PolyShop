package com.polyshop.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderEvents {

    private OrderEvents() {
    }

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED = "stock.reservation_failed";

    public record OrderItem(
            UUID productId,
            UUID variantId,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public record OrderCreatedPayload(
            UUID orderId,
            UUID userId,
            List<OrderItem> items,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt
    ) {}

    public record OrderCancelledPayload(
            UUID orderId,
            String reason,
            Instant cancelledAt
    ) {}

    public record PaymentSucceededPayload(
            UUID orderId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            Instant paidAt
    ) {}

    public record PaymentFailedPayload(
            UUID orderId,
            UUID paymentId,
            String failureReason,
            Instant failedAt
    ) {}

    public record StockReservedPayload(
            UUID orderId,
            UUID reservationId,
            Instant reservedAt
    ) {}

    public record StockReservationFailedPayload(
            UUID orderId,
            String reason
    ) {}
}
