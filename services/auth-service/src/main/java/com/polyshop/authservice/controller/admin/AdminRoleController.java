package com.polyshop.authservice.controller.admin;

import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.dto.AuthApiDtos.SimpleResp;
import com.polyshop.authservice.service.RoleService;
import com.polyshop.authservice.spec.RoleSpecs;
import com.polyshop.authservice.util.SpecUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class AdminRoleController {

    private final RoleService roleService;

    public AdminRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<?> listRoles(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        Specification<Role> spec = null;
        if (name != null) spec = SpecUtil.safeChain(spec, RoleSpecs.hasName(name));
        Page<Role> roles = roleService.searchRoles(spec, PageRequest.of(page, size));
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestParam String name) {
        try {
            Role r = roleService.createRole(name.toUpperCase());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new SimpleResp(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(new SimpleResp("deleted"));
    }
}
