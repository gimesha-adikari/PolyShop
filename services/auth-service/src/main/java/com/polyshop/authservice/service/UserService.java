package com.polyshop.authservice.service;

import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;
import java.util.Set;

public interface UserService {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Page<User> searchUsers(Specification<User> spec, Pageable pageable);
    User register(String username, String email, String phone, String rawPassword, String fullName, boolean asAdmin);
    User authenticate(String usernameOrEmailOrPhone, String rawPassword);
    void changePassword(Long userId, String currentRawPassword, String newRawPassword);
    void revokeAllTokensForUser(Long userId);
    User enableUser(Long userId);
    User disableUser(Long userId);
    User setRoles(Long userId, Set<Role> roles);
    void requestPasswordReset(String email);
    void confirmPasswordReset(String token, String newPassword);
    User save(User user);
    void softDeleteUser(Long userId, String reason);
    void undeleteUser(Long userId);
    void revokeAccessTokens(Long userId);


}
