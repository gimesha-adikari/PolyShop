export interface RegisterRequest {
    email: string;
    password: string;
    fullName?: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
}

export interface PasswordResetRequest {
    email: string;
}

export interface PasswordResetConfirmRequest {
    token: string;
    newPassword: string;
}

export interface EmailVerifyRequest {
    token: string;
}

export interface AuthTokens {
    accessToken: string;
    refreshToken?: string;
    tokenType: string;
    expiresIn: number;
}

export interface AuthResponse extends AuthTokens {
    user: UserResponse;
}

export interface UserResponse {
    id: string;
    email: string;
    fullName?: string;
    roles: string[];
    enabled: boolean;
    emailVerified: boolean;
    createdAt: string;
}
