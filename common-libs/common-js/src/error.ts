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

export class ApiError extends Error {
    status: number;
    code?: string;
    details?: Record<string, unknown>;

    constructor(status: number, message: string, code?: string, details?: Record<string, unknown>) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
}
