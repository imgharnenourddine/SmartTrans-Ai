package com.smarttrans.ai.role;

import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.domain.Role;
import com.smarttrans.ai.domain.RoleCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class RoleService {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    public RoleResponse create(CreateRoleRequest request) {
        if (existsByCode(request.code())) {
            throw new Conflict("Role code already exists: " + request.code());
        }
        Role role = new Role();
        role.setCode(request.code());
        role.setLabel(request.label().trim());
        entityManager.persist(role);
        entityManager.flush();
        return toResponse(role);
    }

    public RoleResponse getById(UUID id) {
        return toResponse(find(id));
    }

    public List<RoleResponse> getAll() {
        return entityManager.createQuery("select r from Role r", Role.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        Role role = find(id);
        if (request.code() != null && request.code() != role.getCode()) {
            if (existsByCode(request.code())) {
                throw new Conflict("Role code already exists: " + request.code());
            }
            role.setCode(request.code());
        }
        if (request.label() != null) {
            role.setLabel(request.label().trim());
        }
        return toResponse(role);
    }

    public void delete(UUID id) {
        Role role = entityManager.find(Role.class, id);
        if (role == null) {
            throw new NotFound("Role not found with id: " + id);
        }
        entityManager.remove(role);
    }

    private Role find(UUID id) {
        Role role = entityManager.find(Role.class, id);
        if (role == null) {
            throw new NotFound("Role not found with id: " + id);
        }
        return role;
    }

    private boolean existsByCode(RoleCode code) {
        Long count = entityManager.createQuery(
                        "select count(r) from Role r where r.code = :code", Long.class)
                .setParameter("code", code)
                .getSingleResult();
        return count > 0;
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getLabel(), role.getCreatedAt());
    }

    public record CreateRoleRequest(@NotNull RoleCode code, @NotBlank @Size(max = 100) String label) {
    }

    public record UpdateRoleRequest(RoleCode code, @Size(max = 100) String label) {
    }

    public record RoleResponse(UUID id, RoleCode code, String label, Instant createdAt) {
    }
}
