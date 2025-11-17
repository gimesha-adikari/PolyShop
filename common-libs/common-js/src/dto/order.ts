import type { Address, PageMetadata } from "./common";

export type OrderStatus =
    | "CREATED"
    | "PENDING_PAYMENT"
    | "PAYMENT_FAILED"
    | "PAID"
    | "FULFILLING"
    | "FULFILLED"
    | "CANCELLED";

export interface OrderItemCreateRequest {
    productId: string;
    variantId?: string | null;
    quantity: number;
}

export interface OrderCreateRequest {
    items: OrderItemCreateRequest[];
    shippingAddress: Address;
    billingAddress?: Address;
}

export interface OrderItemResponse {
    productId: string;
    variantId?: string | null;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
}

export interface OrderResponse {
    id: string;
    userId: string;
    items: OrderItemResponse[];
    totalAmount: number;
    currency: string;
    status: OrderStatus;
    shippingAddress: Address;
    billingAddress?: Address;
    createdAt: string;
    updatedAt: string;
}

export interface PageOrderResponse extends PageMetadata {
    content: OrderResponse[];
}
