package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.domain.enums.Status;
import com.polyshop.authservice.domain.enums.TokenType;
import com.polyshop.authservice.repository.RoleRepository;
import com.polyshop.authservice.repository.UserRepository;
import com.polyshop.authservice.service.AuditService;
import com.polyshop.authservice.service.AuthTokenService;
import com.polyshop.authservice.service.EmailService;
import com.polyshop.authservice.service.UserService;
import com.polyshop.authservice.spec.UserSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenService authTokenService;
    private final EmailService emailService; // injected directly
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final int MAX_FAILED_ATTEMPTS = 5;
    private final long LOCK_DURATION_SECONDS = 60 * 15;
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private final AuditService auditService;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public Page<User> searchUsers(Specification<User> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable);
    }

    public User register(String username, String email, String phone, String rawPassword, String fullName, boolean asAdmin) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone already registered");
        }
        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(normalizedEmail);
        u.setPhone(phone.trim());
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus(Status.ACTIVE);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        u.getRoles().add(userRole);

        if (asAdmin) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            u.getRoles().add(adminRole);
        }

        User saved = userRepository.save(u);
        return saved;
    }

    @Override
    public User authenticate(String usernameOrEmailOrPhone, String rawPassword) {
        Optional<User> maybeUser = Optional.empty();

        // heuristic: if contains '@' treat as email, if all digits treat as phone, otherwise username
        String input = usernameOrEmailOrPhone == null ? "" : usernameOrEmailOrPhone.trim();
        if (input.contains("@")) {
            maybeUser = userRepository.findByEmail(input.toLowerCase());
        } else if (input.matches("\\d+")) {
            maybeUser = userRepository.findByPhone(input);
        } else {
            maybeUser = userRepository.findByUsername(input);
        }

        if (maybeUser.isEmpty()) throw new IllegalArgumentException("invalid credentials");

        User user = maybeUser.get();
        if (user.getStatus() == Status.INACTIVE || user.getStatus() == Status.DELETED) {
            throw new IllegalStateException("Account not active");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new IllegalStateException("Account locked until " + user.getLockedUntil());
        }
        boolean matches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        if (matches) {
            log.info("login_success userId={}", user.getId());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            return user;
        } else {
            log.warn("login_failure usernameOrEmailOrPhone={}", usernameOrEmailOrPhone);
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            }
            userRepository.save(user);
            throw new IllegalArgumentException("invalid credentials");
        }
    }

    @Override
    public void changePassword(Long userId, String currentRawPassword, String newRawPassword) {
        User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("unknown user"));
        if (!passwordEncoder.matches(currentRawPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("current password is invalid");
        }
        u.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(u);
        authTokenService.revokeAllForUser(u.getId());
    }

    @Override
    public void revokeAllTokensForUser(Long userId) {
        authTokenService.revokeAllForUser(userId);
    }

    @Override
    public User enableUser(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(Status.ACTIVE);
        return userRepository.save(u);
    }

    @Override
    public User disableUser(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(Status.INACTIVE);
        authTokenService.revokeAllForUser(u.getId());
        return userRepository.save(u);
    }

    @Override
    public User setRoles(Long userId, Set<Role> roles) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setRoles(roles);
        return userRepository.save(u);
    }

    @Override
    public void requestPasswordReset(String email) {
        var userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) return;
        User u = userOpt.get();
        String token = authTokenService.createToken(u, TokenType.PASSWORD_RESET, 3600);
        String url = "https://your-frontend.example/reset-password?token=" + token;
        emailService.sendEmail(u.getEmail(), "Password reset", url);
    }

    @Override
    public void confirmPasswordReset(String token, String newPassword) {
        var consumed = authTokenService.validateAndConsume(token, TokenType.PASSWORD_RESET);
        User u = consumed.getUser();
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        userRepository.save(u);
        authTokenService.revokeAllForUser(u.getId());
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void softDeleteUser(Long userId, String reason) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(Status.DELETED);
        userRepository.save(u);
        authTokenService.revokeAllForUser(userId);
        auditService.record("USER_DELETED", userId, reason);
        emailService.sendEmail(u.getEmail(), "Account deleted", "Your account was deleted: " + reason);
    }

    @Override
    public void undeleteUser(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(Status.INACTIVE);
        userRepository.save(u);
        auditService.record("USER_UNDELETED", userId, "admin_undo");
    }

    @Override
    public void revokeAccessTokens(Long userId) {
        authTokenService.revokeAllAccessTokensForUser(userId);
    }
}
