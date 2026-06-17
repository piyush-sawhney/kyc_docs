INSERT INTO document_types (id, name, is_active) VALUES
    (gen_random_uuid(), 'PAN', TRUE),
    (gen_random_uuid(), 'Aadhar', TRUE),
    (gen_random_uuid(), 'Passport', TRUE),
    (gen_random_uuid(), 'Driving License', TRUE),
    (gen_random_uuid(), 'Voter ID', TRUE),
    (gen_random_uuid(), 'OCI', TRUE)
ON CONFLICT (name) DO NOTHING;
