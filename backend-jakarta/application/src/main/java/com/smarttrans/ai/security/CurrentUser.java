package com.smarttrans.ai.security;

import com.smarttrans.ai.domain.RoleCode;
import jakarta.enterprise.context.RequestScoped;
import java.util.Set;
import java.util.UUID;

@RequestScoped
public class CurrentUser {

    private UUID id;
    private String email;
    private Set<RoleCode> roles = Set.of();
    private boolean authenticated;

    public void authenticate(UUID id, String email, Set<RoleCode> roles) {
        this.id = id;
        this.email = email;
        this.roles = Set.copyOf(roles);
        this.authenticated = true;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(code -> code.name().equals(role));
    }

    public boolean hasAny(String... required) {
        for (String role : required) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
