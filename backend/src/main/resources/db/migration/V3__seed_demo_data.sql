-- Données de démo persistantes (idempotent).
-- Flyway les applique au démarrage de l’API.
-- Tu peux aussi coller ce fichier dans pgAdmin 4 → Query Tool → Execute (F5).

INSERT INTO clients (company_name, address, phone, email)
SELECT v.company_name, v.address, v.phone, v.email
FROM (VALUES
    ('Atlas Freight', 'Zone industrielle Ain Sebaa, Casablanca', '+212522701001', 'atlas@demo.smarttrans.ai'),
    ('Maghreb Logistics', 'Avenue Hassan II, Rabat', '+212537701002', 'maghreb@demo.smarttrans.ai'),
    ('Sud Transport', 'Port d''Agadir, Agadir', '+212528701003', 'sud@demo.smarttrans.ai'),
    ('Nord Cargo', 'Route de Tanger, Tétouan', '+212539701004', 'nord@demo.smarttrans.ai'),
    ('Oriental Express', 'Boulevard Mohammed VI, Oujda', '+212536701005', 'oriental@demo.smarttrans.ai'),
    ('Sahara Fret', 'Zone portuaire, Laâyoune', '+212528701006', 'sahara@demo.smarttrans.ai'),
    ('Fes Distribution', 'Route de Sefrou, Fès', '+212535701007', 'fes@demo.smarttrans.ai')
) AS v(company_name, address, phone, email)
WHERE NOT EXISTS (
    SELECT 1 FROM clients c WHERE c.company_name = v.company_name
);

INSERT INTO vehicles (
    registration_number, type, capacity_kg, mileage_km, consumption_l_per_100km,
    insurance_expires_at, technical_visit_expires_at, status
)
VALUES
    ('DB-701-A', 'TRUCK', 12000, 85000, 28.5, CURRENT_DATE + 12, CURRENT_DATE + 80, 'AVAILABLE'),
    ('DB-702-A', 'VAN', 3500, 42000, 11.0, CURRENT_DATE + 180, CURRENT_DATE + 200, 'AVAILABLE'),
    ('DB-703-A', 'TRUCK', 18000, 110000, 32.0, CURRENT_DATE + 90, CURRENT_DATE + 120, 'AVAILABLE'),
    ('DB-704-A', 'TRUCK', 15000, 67000, 30.0, CURRENT_DATE + 60, CURRENT_DATE + 45, 'IN_USE'),
    ('DB-705-A', 'VAN', 2800, 31000, 10.5, CURRENT_DATE + 150, CURRENT_DATE + 160, 'IN_USE'),
    ('DB-706-A', 'TRUCK', 20000, 94000, 34.0, CURRENT_DATE + 200, CURRENT_DATE + 210, 'AVAILABLE'),
    ('DB-707-A', 'TRAILER', 25000, 52000, NULL, CURRENT_DATE + 8, CURRENT_DATE + 70, 'AVAILABLE')
ON CONFLICT (registration_number) DO NOTHING;

INSERT INTO drivers (
    first_name, last_name, phone, license_number, license_expires_at, experience_years, status
)
VALUES
    ('Karim', 'Benali', '+212661701001', 'LIC-DB-701', CURRENT_DATE + 15, 8, 'AVAILABLE'),
    ('Fatima', 'Zahra', '+212661701002', 'LIC-DB-702', CURRENT_DATE + 400, 5, 'AVAILABLE'),
    ('Youssef', 'Amrani', '+212661701003', 'LIC-DB-703', CURRENT_DATE + 250, 12, 'AVAILABLE'),
    ('Nadia', 'El Idrissi', '+212661701004', 'LIC-DB-704', CURRENT_DATE + 180, 6, 'ON_MISSION'),
    ('Omar', 'Tazi', '+212661701005', 'LIC-DB-705', CURRENT_DATE + 90, 9, 'ON_MISSION'),
    ('Sara', 'Bennani', '+212661701006', 'LIC-DB-706', CURRENT_DATE + 320, 4, 'AVAILABLE'),
    ('Mehdi', 'Kabbaj', '+212661701007', 'LIC-DB-707', CURRENT_DATE + 500, 11, 'AVAILABLE')
ON CONFLICT (license_number) DO NOTHING;

INSERT INTO missions (
    reference, client_id, driver_id, vehicle_id,
    departure_location, destination, scheduled_at, started_at, completed_at,
    weight_kg, cargo_type, observations, status
)
SELECT
    m.reference,
    c.id,
    d.id,
    v.id,
    m.departure_location,
    m.destination,
    m.scheduled_at,
    m.started_at,
    m.completed_at,
    m.weight_kg,
    m.cargo_type,
    m.observations,
    m.status
FROM (VALUES
    (
        'DB-01', 'Atlas Freight', 'LIC-DB-701', 'DB-701-A',
        'Casablanca', 'Marrakech',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '9 hours',
        NULL::timestamptz, NULL::timestamptz,
        4200.00, 'Textile', 'Démo planifiée', 'PLANIFIED'
    ),
    (
        'DB-02', 'Maghreb Logistics', 'LIC-DB-702', 'DB-702-A',
        'Rabat', 'Fès',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '2 days 10 hours',
        NULL, NULL,
        1800.00, 'Pièces détachées', 'Démo planifiée', 'PLANIFIED'
    ),
    (
        'DB-03', 'Sud Transport', 'LIC-DB-703', 'DB-703-A',
        'Agadir', 'Tanger',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '4 days 14 hours',
        NULL, NULL,
        7600.00, 'Agroalimentaire', 'Démo planifiée', 'PLANIFIED'
    ),
    (
        'DB-04', 'Nord Cargo', 'LIC-DB-704', 'DB-704-A',
        'Casablanca', 'Oujda',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '1 day 8 hours',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '1 day 8 hours',
        NULL,
        9100.00, 'Matériaux', 'Démo en cours', 'IN_PROGRESS'
    ),
    (
        'DB-05', 'Oriental Express', 'LIC-DB-705', 'DB-705-A',
        'Fès', 'Nador',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '3 days 7 hours',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '3 days 7 hours',
        NULL,
        2200.00, 'Colis', 'Démo en cours', 'IN_PROGRESS'
    ),
    (
        'DB-06', 'Sahara Fret', 'LIC-DB-706', 'DB-706-A',
        'Laâyoune', 'Agadir',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '5 days 11 hours',
        NULL, NULL,
        5400.00, 'Équipement', 'Démo annulée', 'CANCELLED'
    ),
    (
        'DB-07', 'Fes Distribution', 'LIC-DB-707', 'DB-707-A',
        'Fès', 'Casablanca',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '8 hours',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '8 hours',
        date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '16 hours',
        3100.00, 'Distribution', 'Démo terminée', 'COMPLETED'
    )
) AS m(
    reference, company_name, license_number, registration_number,
    departure_location, destination, scheduled_at, started_at, completed_at,
    weight_kg, cargo_type, observations, status
)
JOIN clients c ON c.company_name = m.company_name
JOIN drivers d ON d.license_number = m.license_number
JOIN vehicles v ON v.registration_number = m.registration_number
WHERE NOT EXISTS (
    SELECT 1 FROM missions existing WHERE existing.reference = m.reference
);
