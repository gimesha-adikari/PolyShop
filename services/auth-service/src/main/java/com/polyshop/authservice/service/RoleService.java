package com.polyshop.authservice.service;

import com.polyshop.authservice.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;

public interface RoleService {
    Optional<Role> findByName(String name);
    Role createRole(String name);
    Page<Role> searchRoles(Specification<Role> spec, Pageable pageable);
    void deleteRole(Long id);
}
