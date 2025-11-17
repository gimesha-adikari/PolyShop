package com.polyshop.common.security;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class JwtUser {

    private UUID id;
    private String email;
    private List<String> roles;

    public JwtUser() {
    }

    public JwtUser(UUID id, String email, List<String> roles) {
        this.id = id;
        this.email = email;
        this.roles = roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
