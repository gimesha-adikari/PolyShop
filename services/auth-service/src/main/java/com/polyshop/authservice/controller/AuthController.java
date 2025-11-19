package com.polyshop.authservice.controller;

import com.polyshop.authservice.domain.AuthToken;
import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.domain.enums.Status;
import com.polyshop.authservice.domain.enums.TokenType;
import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.dto.AuthApiDtos;
import com.polyshop.authservice.dto.AuthApiDtos.*;
import com.polyshop.authservice.security.TotpUtil;
import com.polyshop.authservice.service.AuthTokenService;
import com.polyshop.authservice.service.EmailService;
import com.polyshop.authservice.service.UserService;
import com.polyshop.authservice.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${auth.jwt.expires-in:900}")
    private long accessTtl;

    @Value("${auth.jwt.refresh-expires-in:2592000}")
    private long refreshTtl;

    @Value("${frontend.base-url:https://polyshop.example}")
    private String frontendBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        if (req == null || req.email == null || req.password == null || req.username == null) {
            return ResponseEntity.badRequest().body(new SimpleResp("username,email,password required"));
        }
        String email = req.email.trim().toLowerCase();
        var existingOpt = userService.findByEmail(email);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == Status.DELETED) {
            return ResponseEntity.status(409).body(new SimpleResp("ACCOUNT_DELETED"));
        }
        try {
            User u = userService.register(req.username, email, req.phone, req.password, req.fullName, req.asAdmin);
            String emailToken = authTokenService.createEmailVerificationToken(u);
            String verifyUrl = frontendBaseUrl + "/verify-email?token=" + emailToken;
            emailService.sendEmail(u.getEmail(), "Verify your email", verifyUrl);
            return ResponseEntity.ok(new SimpleResp("registered"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(409).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        if (req == null || req.usernameOrEmailOrPhone == null || req.password == null) {
            return ResponseEntity.badRequest().body(new SimpleResp("username/email/phone and password required"));
        }
        try {
            User u = userService.authenticate(req.usernameOrEmailOrPhone, req.password);
            List<String> roles = u.getRoles().stream().map(r -> r.getName()).toList();
            String jti = UUID.randomUUID().toString();
            String access = jwtUtil.generateAccessToken(u.getEmail(), roles, jti);
            authTokenService.createAccessTokenEntry(u, jti, accessTtl);
            String refresh = authTokenService.createRefreshToken(u, refreshTtl);
            return ResponseEntity.ok(new TokenResp(access, refresh, accessTtl));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(401).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshReq req) {
        if (req == null || req.refreshToken == null) return ResponseEntity.badRequest().body(new SimpleResp("refreshToken required"));
        try {
            AuthToken old = authTokenService.validateToken(req.refreshToken, TokenType.REFRESH);
            String rotated = authTokenService.rotateRefreshToken(req.refreshToken, refreshTtl);
            User u = old.getUser();
            List<String> roles = u.getRoles().stream().map(r -> r.getName()).toList();
            String jti = UUID.randomUUID().toString();
            String access = jwtUtil.generateAccessToken(u.getEmail(), roles, jti);
            authTokenService.createAccessTokenEntry(u, jti, accessTtl);
            return ResponseEntity.ok(new TokenResp(access, rotated, accessTtl));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam("email") String email) {
        var maybe = userService.findByEmail(email);
        if (maybe.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("unknown user"));
        userService.revokeAllTokensForUser(maybe.get().getId());
        return ResponseEntity.ok(new SimpleResp("logged out"));
    }

    @PostMapping("/request-account-restore")
    public ResponseEntity<?> requestAccountRestore(@Valid @RequestBody RequestRestoreReq req) {
        if (req == null || req.email == null) return ResponseEntity.badRequest().body(new SimpleResp("email required"));
        var maybe = userService.findByEmail(req.email.trim().toLowerCase());
        if (maybe.isEmpty() || maybe.get().getStatus() != Status.DELETED) {
            return ResponseEntity.badRequest().body(new SimpleResp("no deleted account found for the provided email"));
        }
        User deletedUser = maybe.get();
        String token = authTokenService.createAccountRestoreToken(deletedUser);
        String restoreUrl = frontendBaseUrl + "/restore-account?token=" + token;
        emailService.sendEmail(deletedUser.getEmail(), "Restore your PolyShop account", restoreUrl);
        return ResponseEntity.ok(new SimpleResp("restore email sent"));
    }

    @PostMapping("/restore-account")
    @Transactional
    public ResponseEntity<?> restoreAccount(@Valid @RequestBody RestoreReq req) {
        if (req == null || req.token == null) return ResponseEntity.badRequest().body(new SimpleResp("token required"));
        try {
            AuthToken consumed = authTokenService.validateAndConsume(req.token, TokenType.ACCOUNT_RESTORE);
            User user = consumed.getUser();
            user.setStatus(Status.ACTIVE);
            user.setEmailVerified(true);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            if (req.newPassword != null && !req.newPassword.isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(req.newPassword));
            }
            userService.save(user);
            userService.revokeAllTokensForUser(user.getId());
            userService.enableUser(user.getId());

            List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();
            String jti = UUID.randomUUID().toString();
            String access = jwtUtil.generateAccessToken(user.getEmail(), roles, jti);
            authTokenService.createAccessTokenEntry(user, jti, accessTtl);
            String refresh = authTokenService.createRefreshToken(user, refreshTtl);
            return ResponseEntity.ok(new TokenResp(access, refresh, accessTtl));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/request-email-verify")
    public ResponseEntity<?> requestEmailVerify(@Valid @RequestBody RequestEmailVerifyReq req) {
        if (req == null || req.email == null) return ResponseEntity.badRequest().body(new SimpleResp("email required"));
        var maybe = userService.findByEmail(req.email.trim().toLowerCase());
        if (maybe.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("unknown user"));
        User u = maybe.get();
        String token = authTokenService.createEmailVerificationToken(u);
        String verifyUrl = frontendBaseUrl + "/verify-email?token=" + token;
        emailService.sendEmail(u.getEmail(), "Verify your email", verifyUrl);
        return ResponseEntity.ok(new SimpleResp("verification email sent"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailReq req) {
        if (req == null || req.token == null) return ResponseEntity.badRequest().body(new SimpleResp("token required"));
        try {
            AuthToken consumed = authTokenService.validateAndConsume(req.token, TokenType.EMAIL_VERIFICATION);
            User user = consumed.getUser();
            user.setEmailVerified(true);
            userService.save(user);
            userService.revokeAccessTokens(user.getId());
            userService.enableUser(user.getId());
            return ResponseEntity.ok(new SimpleResp("email verified"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/request-phone-otp")
    public ResponseEntity<?> requestPhoneOtp(@Valid @RequestBody RequestPhoneOtpReq req) {
        if (req == null || req.phone == null) return ResponseEntity.badRequest().body(new SimpleResp("phone required"));
        var list = userService.searchUsers(com.polyshop.authservice.spec.UserSpecs.hasPhone(req.phone), org.springframework.data.domain.Pageable.unpaged()).getContent();
        if (list.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("unknown phone"));
        User u = list.get(0);
        String token = authTokenService.createToken(u, TokenType.PHONE_OTP, 300);
        log.info("PHONE OTP token for {} -> {}", u.getPhone(), token);
        return ResponseEntity.ok(new SimpleResp("otp sent"));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@Valid @RequestBody VerifyPhoneReq req) {
        if (req == null || req.token == null) return ResponseEntity.badRequest().body(new SimpleResp("token required"));
        try {
            AuthToken consumed = authTokenService.validateAndConsume(req.token, TokenType.PHONE_OTP);
            User user = consumed.getUser();
            if (req.phone == null || !req.phone.equals(user.getPhone())) {
                return ResponseEntity.badRequest().body(new SimpleResp("phone_mismatch"));
            }
            user.setPhoneVerified(true);
            userService.save(user);
            userService.revokeAccessTokens(user.getId());
            userService.enableUser(user.getId());
            return ResponseEntity.ok(new SimpleResp("phone verified"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(new SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody AuthApiDtos.PasswordResetReq req) {
        if (req == null || req.email == null) return ResponseEntity.badRequest().body(new AuthApiDtos.SimpleResp("email required"));
        userService.requestPasswordReset(req.email);
        return ResponseEntity.ok(new AuthApiDtos.SimpleResp("password reset email sent"));
    }

    @PostMapping("/confirm-password-reset")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody AuthApiDtos.PasswordResetConfirmReq req) {
        if (req == null || req.token == null || req.newPassword == null) return ResponseEntity.badRequest().body(new AuthApiDtos.SimpleResp("token and newPassword required"));
        try {
            userService.confirmPasswordReset(req.token, req.newPassword);
            return ResponseEntity.ok(new AuthApiDtos.SimpleResp("password reset"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(new AuthApiDtos.SimpleResp(ex.getMessage()));
        }
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<?> mfaSetup(@RequestParam String email) {
        var u = userService.findByEmail(email.trim().toLowerCase()).orElseThrow(() -> new IllegalArgumentException("unknown user"));
        String secret = TotpUtil.generateBase32Secret();
        u.setMfaSecret(secret);
        userService.save(u);
        String otpAuth = TotpUtil.getOtpAuthUrl(u.getEmail(), "PolyShop", secret);
        return ResponseEntity.ok(Map.of("secret", secret, "otpAuthUrl", otpAuth));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<?> mfaEnable(@RequestParam String email, @RequestParam int code) {
        var u = userService.findByEmail(email.trim().toLowerCase()).orElseThrow();
        if (u.getMfaSecret() == null) return ResponseEntity.badRequest().body(new SimpleResp("mfa_not_setup"));
        boolean ok = TotpUtil.verifyCode(u.getMfaSecret(), code, 30, 1);
        if (!ok) return ResponseEntity.badRequest().body(new SimpleResp("invalid_code"));
        u.setMfaEnabled(true);
        userService.save(u);
        return ResponseEntity.ok(new SimpleResp("mfa_enabled"));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<?> mfaDisable(@RequestParam String email, @RequestParam int code) {
        var u = userService.findByEmail(email.trim().toLowerCase()).orElseThrow();
        if (!u.isMfaEnabled()) return ResponseEntity.badRequest().body(new SimpleResp("mfa_not_enabled"));
        boolean ok = TotpUtil.verifyCode(u.getMfaSecret(), code, 30, 1);
        if (!ok) return ResponseEntity.badRequest().body(new SimpleResp("invalid_code"));
        u.setMfaEnabled(false);
        u.setMfaSecret(null);
        userService.save(u);
        return ResponseEntity.ok(new SimpleResp("mfa_disabled"));
    }
}
