package com.smarttrans.ai.security;

import com.smarttrans.ai.api.ApiExceptions.Forbidden;
import com.smarttrans.ai.api.ApiExceptions.Unauthorized;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Access {

    @Inject
    private CurrentUser currentUser;

    public void requireAny(String... roles) {
        if (!currentUser.isAuthenticated()) {
            throw new Unauthorized("Authentication required");
        }
        if (!currentUser.hasAny(roles)) {
            throw new Forbidden();
        }
    }

    public CurrentUser currentUser() {
        if (!currentUser.isAuthenticated()) {
            throw new Unauthorized("Authentication required");
        }
        return currentUser;
    }
}
