import type { ErrorResponse } from "./common";

export type PaymentProvider = "STRIPE" | "PAYPAL";

export type PaymentStatus =
    | "INITIATED"
    | "REQUIRES_ACTION"
    | "SUCCESS"
    | "FAILED"
    | "REFUNDED";

export interface PaymentCreateRequest {
    orderId: string;
    provider: PaymentProvider;
    successUrl?: string;
    cancelUrl?: string;
}

export interface PaymentResponse {
    id: string;
    orderId: string;
    amount: number;
    currency: string;
    status: PaymentStatus;
    provider: string;
    providerRef?: string;
    checkoutUrl?: string | null;
    createdAt: string;
}

export interface RefundCreateRequest {
    amount?: number;
    reason?: string;
}

export type RefundStatus = "PENDING" | "SUCCESS" | "FAILED";

export interface RefundResponse {
    id: string;
    paymentId: string;
    amount: number;
    status: RefundStatus;
    createdAt: string;
}

export type PaymentError = ErrorResponse;
