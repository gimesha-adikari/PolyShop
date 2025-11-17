import type { PageMetadata } from "./common";

export interface UserResponse {
    id: string;
    email: string;
    fullName?: string;
    roles: string[];
    enabled: boolean;
    emailVerified: boolean;
    createdAt: string;
}

export interface PageUserResponse extends PageMetadata {
    content: UserResponse[];
}

export interface SessionResponse {
    id: string;
    userAgent?: string | null;
    ipAddress?: string | null;
    createdAt: string;
    lastUsedAt: string;
    current: boolean;
}
