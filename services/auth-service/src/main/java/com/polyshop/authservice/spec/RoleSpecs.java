package com.polyshop.authservice.spec;

import com.polyshop.authservice.domain.Role;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecs {
    public static Specification<Role> hasName(String name) {
        return SearchSpecifications.equalsIgnoreCase("name", name);
    }
}
