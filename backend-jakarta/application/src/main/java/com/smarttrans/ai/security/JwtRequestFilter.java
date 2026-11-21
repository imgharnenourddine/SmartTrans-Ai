package com.smarttrans.ai.security;

import com.smarttrans.ai.api.ApiExceptionMapper;
import com.smarttrans.ai.domain.Role;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class JwtRequestFilter implements ContainerRequestFilter {

    private static final String BEARER = "Bearer ";

    @Inject
    private JwtTokens tokens;

    @Inject
    private CurrentUser currentUser;

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Override
    @Transactional
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getRequestUri().getPath();
        if (isPublic(path)) {
            return;
        }
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            abort(request, path);
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        if (token.isEmpty() || !tokens.isValid(token)) {
            abort(request, path);
            return;
        }
        String email = tokens.parse(token).get("email", String.class);
        if (email == null || email.isBlank()) {
            abort(request, path);
            return;
        }
        List<UserAccount> users = entityManager.createQuery(
                        "select distinct u from UserAccount u left join fetch u.roles where u.email = :email",
                        UserAccount.class)
                .setParameter("email", email.trim().toLowerCase())
                .getResultList();
        if (users.isEmpty()) {
            abort(request, path);
            return;
        }
        UserAccount user = users.getFirst();
        Set<RoleCode> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        currentUser.authenticate(user.getId(), user.getEmail(), roles);
    }

    private boolean isPublic(String path) {
        return "/actuator/health".equals(path)
                || "/api/v1/platform".equals(path)
                || path.startsWith("/api/v1/auth/");
    }

    private void abort(ContainerRequestContext request, String path) {
        ApiExceptionMapper.ApiError body = new ApiExceptionMapper.ApiError(
                Instant.now(), 401, "Unauthorized", "Authentication required", path, null);
        request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type("application/json")
                .entity(body)
                .build());
    }
}
