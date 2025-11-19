package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.domain.Role;
import com.polyshop.authservice.repository.RoleRepository;
import com.polyshop.authservice.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void ensureDefaultRoles() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) roleRepository.save(new Role("ROLE_USER"));
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) roleRepository.save(new Role("ROLE_ADMIN"));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public Role createRole(String name) {
        if (roleRepository.findByName(name).isPresent()) throw new IllegalArgumentException("role exists");
        Role r = new Role(name);
        return roleRepository.save(r);
    }

    @Override
    public Page<Role> searchRoles(Specification<Role> spec, Pageable pageable) {
        return roleRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
