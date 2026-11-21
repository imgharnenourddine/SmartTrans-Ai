package com.smarttrans.ai.auth;

import com.smarttrans.ai.api.ApiExceptions.Unauthorized;
import com.smarttrans.ai.domain.Role;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.security.JwtTokens;
import com.smarttrans.ai.security.Passwords;
import com.smarttrans.ai.user.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private UserService users;

    @Inject
    private Passwords passwords;

    @Inject
    private JwtTokens tokens;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public LoginResponse login(@Valid LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        UserAccount user = users.findByEmail(email);
        if (user == null) {
            throw new Unauthorized("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new Unauthorized("User account is disabled");
        }
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new Unauthorized("Invalid email or password");
        }
        users.recordLogin(user.getId());
        Set<RoleCode> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        List<String> roleNames = roleCodes.stream().map(RoleCode::name).toList();
        String accessToken = tokens.issue(user.getId(), user.getEmail(), roleNames);
        return new LoginResponse(
                accessToken,
                "Bearer",
                tokens.expirationMs(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleCodes);
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresInMs,
            UUID userId,
            String email,
            String firstName,
            String lastName,
            Set<RoleCode> roleCodes) {
    }
}
