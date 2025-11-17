export interface ErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    code?: string;
    message: string;
    path?: string;
    requestId?: string;
    details?: Record<string, unknown>;
}

export interface PageMetadata {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface Address {
    line1: string;
    line2?: string | null;
    city: string;
    state?: string | null;
    postalCode?: string | null;
    country: string;
}

export type HealthStatusValue = "UP" | "DOWN" | "UNKNOWN";

export interface HealthStatus {
    status: HealthStatusValue;
    details?: Record<string, unknown>;
}
