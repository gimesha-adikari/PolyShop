export const REQUEST_ID_HEADER = "X-Request-Id";
export const IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
export const AUTH_HEADER = "Authorization";

export interface RequestContext {
    requestId?: string;
    idempotencyKey?: string;
    accessToken?: string;
}

export function buildOutgoingHeaders(ctx: RequestContext): Record<string, string> {
    const headers: Record<string, string> = {};

    if (ctx.requestId) {
        headers[REQUEST_ID_HEADER] = ctx.requestId;
    }
    if (ctx.idempotencyKey) {
        headers[IDEMPOTENCY_KEY_HEADER] = ctx.idempotencyKey;
    }
    if (ctx.accessToken) {
        headers[AUTH_HEADER] = `Bearer ${ctx.accessToken}`;
    }

    return headers;
}
