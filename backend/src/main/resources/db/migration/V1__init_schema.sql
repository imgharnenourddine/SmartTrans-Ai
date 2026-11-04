-- SmartTrans-AI V1 schema
-- Source of truth for PostgreSQL. Hibernate ddl-auto=validate.

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id     UUID         NOT NULL,
    role_id     UUID         NOT NULL,
    assigned_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE TABLE drivers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    phone               VARCHAR(30)  NOT NULL,
    license_number      VARCHAR(50)  NOT NULL,
    license_expires_at  DATE         NOT NULL,
    experience_years    SMALLINT,
    status              VARCHAR(30)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_drivers_user_id UNIQUE (user_id),
    CONSTRAINT uq_drivers_license_number UNIQUE (license_number),
    CONSTRAINT fk_drivers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_drivers_experience_years CHECK (experience_years IS NULL OR experience_years >= 0),
    CONSTRAINT ck_drivers_status CHECK (status IN ('AVAILABLE', 'ON_MISSION', 'UNAVAILABLE', 'SUSPENDED'))
);

CREATE TABLE vehicles (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_number         VARCHAR(20)    NOT NULL,
    type                        VARCHAR(30)    NOT NULL,
    capacity_kg                 NUMERIC(12, 2),
    mileage_km                  INTEGER        NOT NULL DEFAULT 0,
    consumption_l_per_100km     NUMERIC(6, 2),
    insurance_expires_at        DATE,
    technical_visit_expires_at  DATE,
    status                      VARCHAR(30)    NOT NULL,
    created_at                  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_vehicles_registration_number UNIQUE (registration_number),
    CONSTRAINT ck_vehicles_capacity_kg CHECK (capacity_kg IS NULL OR capacity_kg > 0),
    CONSTRAINT ck_vehicles_mileage_km CHECK (mileage_km >= 0),
    CONSTRAINT ck_vehicles_consumption CHECK (consumption_l_per_100km IS NULL OR consumption_l_per_100km > 0),
    CONSTRAINT ck_vehicles_type CHECK (type IN ('TRUCK', 'VAN', 'TRAILER', 'OTHER')),
    CONSTRAINT ck_vehicles_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_SERVICE'))
);

CREATE TABLE clients (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name  VARCHAR(255) NOT NULL,
    address       TEXT         NOT NULL,
    phone         VARCHAR(30)  NOT NULL,
    email         VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE missions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference           VARCHAR(30)    NOT NULL,
    client_id           UUID           NOT NULL,
    driver_id           UUID,
    vehicle_id          UUID,
    created_by          UUID,
    departure_location  VARCHAR(255)   NOT NULL,
    destination         VARCHAR(255)   NOT NULL,
    scheduled_at        TIMESTAMPTZ    NOT NULL,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    weight_kg           NUMERIC(12, 2),
    cargo_type          VARCHAR(100),
    observations        TEXT,
    status              VARCHAR(30)    NOT NULL DEFAULT 'PLANIFIED',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_missions_reference UNIQUE (reference),
    CONSTRAINT fk_missions_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_missions_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_missions_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_missions_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_missions_weight_kg CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT ck_missions_status CHECK (status IN ('PLANIFIED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(255)  NOT NULL,
    type          VARCHAR(40)   NOT NULL,
    storage_key   VARCHAR(512),
    mime_type     VARCHAR(100),
    size_bytes    BIGINT,
    client_id     UUID,
    mission_id    UUID,
    uploaded_by   UUID,
    rag_status    VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_documents_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_mission FOREIGN KEY (mission_id) REFERENCES missions (id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_documents_size_bytes CHECK (size_bytes IS NULL OR size_bytes >= 0),
    CONSTRAINT ck_documents_type CHECK (type IN (
        'CONTRACT', 'DELIVERY_NOTE', 'INVOICE', 'QUOTE', 'ADMIN', 'INTERNAL_PROCEDURE'
    )),
    CONSTRAINT ck_documents_rag_status CHECK (rag_status IN ('PENDING', 'INDEXED', 'FAILED', 'SKIPPED'))
);

CREATE TABLE document_chunks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id      UUID         NOT NULL,
    chunk_index      INTEGER      NOT NULL,
    content          TEXT         NOT NULL,
    token_count      INTEGER,
    qdrant_point_id  UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_chunks_doc_index UNIQUE (document_id, chunk_index),
    CONSTRAINT uq_document_chunks_qdrant_point_id UNIQUE (qdrant_point_id),
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT ck_document_chunks_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_document_chunks_token_count CHECK (token_count IS NULL OR token_count >= 0)
);

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    mission_id   UUID,
    document_id  UUID,
    vehicle_id   UUID,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_mission FOREIGN KEY (mission_id) REFERENCES missions (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE SET NULL,
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'MISSION_CREATED',
        'MISSION_UPDATED',
        'MISSION_COMPLETED',
        'DOCUMENT_ADDED',
        'INSURANCE_EXPIRING',
        'TECHNICAL_VISIT_EXPIRING'
    ))
);

CREATE TABLE activities (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id  UUID,
    action         VARCHAR(50)  NOT NULL,
    entity_type    VARCHAR(50)  NOT NULL,
    entity_id      UUID,
    metadata       JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_activities_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_missions_status ON missions (status);
CREATE INDEX idx_missions_scheduled_at ON missions (scheduled_at);
CREATE INDEX idx_missions_client_id ON missions (client_id);
CREATE INDEX idx_missions_driver_id ON missions (driver_id);
CREATE INDEX idx_missions_vehicle_id ON missions (vehicle_id);

CREATE INDEX idx_vehicles_status ON vehicles (status);
CREATE INDEX idx_vehicles_insurance_expires_at ON vehicles (insurance_expires_at);
CREATE INDEX idx_vehicles_technical_visit_expires_at ON vehicles (technical_visit_expires_at);

CREATE INDEX idx_drivers_status ON drivers (status);
CREATE INDEX idx_drivers_license_expires_at ON drivers (license_expires_at);

CREATE INDEX idx_documents_type ON documents (type);
CREATE INDEX idx_documents_rag_status ON documents (rag_status);
CREATE INDEX idx_documents_client_id ON documents (client_id);
CREATE INDEX idx_documents_mission_id ON documents (mission_id);

CREATE INDEX idx_document_chunks_document_id ON document_chunks (document_id);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_at);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);

CREATE INDEX idx_activities_entity ON activities (entity_type, entity_id);
CREATE INDEX idx_activities_actor_user_id ON activities (actor_user_id);
CREATE INDEX idx_activities_created_at ON activities (created_at DESC);

-- Seed roles
INSERT INTO roles (code, label) VALUES
    ('ADMIN', 'Administrateur'),
    ('TRANSPORT_MANAGER', 'Responsable transport'),
    ('DRIVER', 'Chauffeur');
