package com.smarttrans.ai.user;

import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Role;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.security.Passwords;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class UserService {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Inject
    private Passwords passwords;

    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (existsByEmail(email)) {
            throw new Conflict("Email already in use: " + email);
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwords.hash(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(normalizePhone(request.phone()));
        user.setEnabled(true);
        user.setRoles(resolveRoles(request.roleCodes()));
        entityManager.persist(user);
        entityManager.flush();
        return toResponse(user);
    }

    public UserResponse getById(UUID id) {
        return toResponse(findWithRoles(id));
    }

    public List<UserResponse> getAll() {
        return entityManager.createQuery(
                        "select distinct u from UserAccount u left join fetch u.roles", UserAccount.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserAccount user = findWithRoles(id);
        if (request.email() != null) {
            String email = request.email().trim().toLowerCase();
            if (!email.equalsIgnoreCase(user.getEmail()) && existsByEmail(email)) {
                throw new Conflict("Email already in use: " + email);
            }
            user.setEmail(email);
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(normalizePhone(request.phone()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.roleCodes() != null) {
            user.setRoles(resolveRoles(request.roleCodes()));
        }
        return toResponse(user);
    }

    public void delete(UUID id) {
        UserAccount user = entityManager.find(UserAccount.class, id);
        if (user == null) {
            throw new NotFound("User not found with id: " + id);
        }
        entityManager.remove(user);
    }

    public UserAccount findByEmail(String email) {
        List<UserAccount> users = entityManager.createQuery(
                        "select distinct u from UserAccount u left join fetch u.roles where u.email = :email",
                        UserAccount.class)
                .setParameter("email", email)
                .getResultList();
        return users.isEmpty() ? null : users.getFirst();
    }

    public void recordLogin(UUID id) {
        UserAccount user = entityManager.find(UserAccount.class, id);
        if (user != null) {
            user.setLastLoginAt(Instant.now());
        }
    }

    private UserAccount findWithRoles(UUID id) {
        List<UserAccount> users = entityManager.createQuery(
                        "select distinct u from UserAccount u left join fetch u.roles where u.id = :id",
                        UserAccount.class)
                .setParameter("id", id)
                .getResultList();
        if (users.isEmpty()) {
            throw new NotFound("User not found with id: " + id);
        }
        return users.getFirst();
    }

    private boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "select count(u) from UserAccount u where u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    private Set<Role> resolveRoles(Set<RoleCode> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new Business("At least one role is required");
        }
        List<Role> roles = entityManager.createQuery(
                        "select r from Role r where r.code in :codes", Role.class)
                .setParameter("codes", roleCodes)
                .getResultList();
        if (roles.size() != roleCodes.size()) {
            Set<RoleCode> found = roles.stream().map(Role::getCode).collect(Collectors.toSet());
            Set<RoleCode> missing = roleCodes.stream()
                    .filter(code -> !found.contains(code))
                    .collect(Collectors.toSet());
            throw new Business("Unknown role code(s): " + missing);
        }
        return new HashSet<>(roles);
    }

    private UserResponse toResponse(UserAccount user) {
        Set<RoleCode> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isEnabled(),
                user.getLastLoginAt(),
                roleCodes,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @NotEmpty Set<RoleCode> roleCodes) {
    }

    public record UpdateUserRequest(
            @Email @Size(max = 255) String email,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            Boolean enabled,
            Set<RoleCode> roleCodes) {
    }

    public record UserResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String phone,
            boolean enabled,
            Instant lastLoginAt,
            Set<RoleCode> roleCodes,
            Instant createdAt,
            Instant updatedAt) {
    }
}
