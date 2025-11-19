package com.polyshop.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthApiDtos {

    public static class RegisterReq {
        @NotBlank
        public String username;
        @NotBlank
        @Email
        public String email;
        @NotBlank
        public String phone;
        @NotBlank
        @Size(min = 8)
        public String password;
        @NotBlank
        public String fullName;
        public boolean asAdmin;
    }

    public static class LoginReq {
        @NotBlank
        public String usernameOrEmailOrPhone;
        @NotBlank
        public String password;
    }

    public static class RefreshReq {
        @NotBlank
        public String refreshToken;
    }

    public static class TokenResp {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public TokenResp() {}
        public TokenResp(String accessToken, String refreshToken, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    public static class SimpleResp {
        public String message;
        public SimpleResp() {}
        public SimpleResp(String message) { this.message = message; }
    }

    public static class RequestRestoreReq {
        @NotBlank
        @Email
        public String email;
    }

    public static class RestoreReq {
        @NotBlank
        public String token;
        @NotBlank
        @Size(min = 8)
        public String newPassword;
    }

    public static class RequestEmailVerifyReq {
        @NotBlank
        @Email
        public String email;
    }

    public static class VerifyEmailReq {
        @NotBlank
        public String token;
    }

    public static class RequestPhoneOtpReq {
        @NotBlank
        public String phone;
    }

    public static class VerifyPhoneReq {
        @NotBlank
        public String token;
        @NotBlank
        public String phone;
    }

    public static class PasswordResetReq {
        @NotBlank
        @Email
        public String email;
    }

    public static class PasswordResetConfirmReq {
        @NotBlank
        public String token;
        @NotBlank
        @Size(min = 8)
        public String newPassword;
    }

}
