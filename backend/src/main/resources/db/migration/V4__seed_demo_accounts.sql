-- Comptes de démonstration (idempotent).
-- Empreinte BCrypt uniquement (BCryptPasswordEncoder, force 10).
-- Les identifiants en clair sont dans docs/presentation/demo-guide.md.
-- N’écrase pas un compte existant : insertion seulement si l’e-mail est absent.

INSERT INTO users (email, password_hash, first_name, last_name, phone, enabled)
SELECT v.email, v.password_hash, v.first_name, v.last_name, v.phone, TRUE
FROM (VALUES
    (
        'admin@demo.smarttrans.ai',
        '$2a$10$sogBSnZyjSYIscye.ZK52uofKZenJ5R/3iYp/GyEGJKhgRvoAVgM6',
        'Amina',
        'El Fassi',
        '+212600000001'
    ),
    (
        'responsable@demo.smarttrans.ai',
        '$2a$10$sogBSnZyjSYIscye.ZK52uofKZenJ5R/3iYp/GyEGJKhgRvoAVgM6',
        'Hassan',
        'Berrada',
        '+212600000002'
    ),
    (
        'chauffeur@demo.smarttrans.ai',
        '$2a$10$sogBSnZyjSYIscye.ZK52uofKZenJ5R/3iYp/GyEGJKhgRvoAVgM6',
        'Fatima',
        'Zahra',
        '+212661701002'
    )
) AS v(email, password_hash, first_name, last_name, phone)
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = v.email
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON (
    (u.email = 'admin@demo.smarttrans.ai' AND r.code = 'ADMIN')
    OR (u.email = 'responsable@demo.smarttrans.ai' AND r.code = 'TRANSPORT_MANAGER')
    OR (u.email = 'chauffeur@demo.smarttrans.ai' AND r.code = 'DRIVER')
)
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Le compte chauffeur correspond à Fatima Zahra (permis LIC-DB-702, mission DB-02).
UPDATE drivers d
SET user_id = u.id,
    updated_at = now()
FROM users u
WHERE u.email = 'chauffeur@demo.smarttrans.ai'
  AND d.license_number = 'LIC-DB-702'
  AND (d.user_id IS NULL OR d.user_id = u.id);

UPDATE missions m
SET created_by = u.id
FROM users u
WHERE u.email = 'responsable@demo.smarttrans.ai'
  AND m.reference IN ('DB-01', 'DB-02', 'DB-03', 'DB-04', 'DB-05', 'DB-06', 'DB-07')
  AND m.created_by IS NULL;

INSERT INTO activities (actor_user_id, action, entity_type, entity_id, metadata)
SELECT u.id, 'CREATE', 'MISSION', m.id,
       jsonb_build_object('reference', m.reference, 'status', m.status)
FROM users u
JOIN missions m ON m.reference IN ('DB-01', 'DB-02', 'DB-04')
WHERE u.email = 'responsable@demo.smarttrans.ai'
  AND NOT EXISTS (
      SELECT 1 FROM activities a
      WHERE a.entity_type = 'MISSION'
        AND a.action = 'CREATE'
        AND a.metadata->>'reference' = m.reference
  );
