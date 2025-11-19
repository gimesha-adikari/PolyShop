package com.polyshop.authservice.controller.admin;

import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.domain.enums.Status;
import com.polyshop.authservice.dto.AuthApiDtos.SimpleResp;
import com.polyshop.authservice.service.UserService;
import com.polyshop.authservice.service.RoleService;
import com.polyshop.authservice.spec.UserSpecs;
import com.polyshop.authservice.util.SpecUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<?> listUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        Specification<User> spec = null;
        if (email != null) spec = SpecUtil.safeChain(spec, UserSpecs.hasEmail(email));
        if (username != null) spec = SpecUtil.safeChain(spec, UserSpecs.hasUsername(username));
        if (phone != null) spec = SpecUtil.safeChain(spec, UserSpecs.hasPhone(phone));
        Page<User> result = userService.searchUsers(spec, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        var u = userService.findById(id);
        if (u.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("not found"));
        return ResponseEntity.ok(u.get());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        var u = userService.findById(id);
        if (u.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("not found"));

        switch (status.toUpperCase()) {
            case "ACTIVE" -> {
                var current = userService.enableUser(id);
                return ResponseEntity.ok(current);
            }
            case "INACTIVE" -> {
                var current = userService.disableUser(id);
                return ResponseEntity.ok(current);
            }
            case "DELETED" -> {
                userService.softDeleteUser(id, "admin_delete");
                return ResponseEntity.ok(new SimpleResp("deleted"));
            }
            default -> {
                return ResponseEntity.badRequest().body(new SimpleResp("invalid_status"));
            }
        }
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<?> setRoles(@PathVariable Long id, @RequestBody Set<String> roles) {
        var u = userService.findById(id);
        if (u.isEmpty()) return ResponseEntity.badRequest().body(new SimpleResp("not found"));
        Set<Role> mapped = roles.stream()
                .map(r -> roleService.findByName(r.toUpperCase())
                        .orElseThrow(() -> new IllegalArgumentException("role_not_found")))
                .collect(Collectors.toSet());
        User saved = userService.setRoles(id, mapped);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestParam(required=false) String reason) {
        userService.softDeleteUser(id, reason == null ? "admin_delete" : reason);
        return ResponseEntity.ok(new SimpleResp("deleted"));
    }

}
