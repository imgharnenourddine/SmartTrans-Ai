package com.smarttrans.ai.bootstrap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Phase 1 schema probe mapped to the existing {@code roles} table.
 * It exists so Hibernate {@code validate} checks a real table and so the runtime can
 * execute one Jakarta Persistence read. It is not the business Role model.
 * Remove it when the real Role entity is migrated, otherwise both mappings will collide.
 */
@Entity
@Table(name = "roles")
public class RoleSchemaProbe {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
