import { randomUUID } from "crypto";

export function newUuid(): string {
    return randomUUID();
}

export function newIdempotencyKey(prefix = "polyshop"): string {
    return `${prefix}_${randomUUID()}`;
}
