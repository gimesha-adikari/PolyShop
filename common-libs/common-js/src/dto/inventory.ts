export interface StockResponse {
    productId: string;
    variantId?: string | null;
    available: number;
    reserved: number;
    updatedAt: string;
}

export interface ReservationRequest {
    orderId: string;
    productId: string;
    variantId?: string | null;
    quantity: number;
    ttlSeconds?: number;
}

export type ReservationStatus =
    | "RESERVED"
    | "CONFIRMED"
    | "RELEASED"
    | "EXPIRED";

export interface ReservationResponse {
    id: string;
    orderId: string;
    productId: string;
    variantId?: string | null;
    quantity: number;
    status: ReservationStatus;
    expiresAt?: string | null;
    createdAt: string;
}

export type StockMovementReason =
    | "PURCHASE"
    | "RETURN"
    | "MANUAL_ADJUST"
    | "CORRECTION";

export interface StockMovementCreateRequest {
    productId: string;
    variantId?: string | null;
    quantityDelta: number;
    reason: StockMovementReason;
}

export interface StockMovementResponse {
    id: string;
    productId: string;
    variantId?: string | null;
    quantityDelta: number;
    reason: string;
    createdAt: string;
}
