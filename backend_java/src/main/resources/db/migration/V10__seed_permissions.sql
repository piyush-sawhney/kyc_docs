INSERT INTO permissions (id, key, label, "group") VALUES
    -- Client permissions
    (gen_random_uuid(), 'client:create', 'Create Client', 'Clients'),
    (gen_random_uuid(), 'client:read', 'Read Client', 'Clients'),
    (gen_random_uuid(), 'client:update', 'Update Client', 'Clients'),
    (gen_random_uuid(), 'client:delete', 'Delete Client', 'Clients'),
    -- Document permissions
    (gen_random_uuid(), 'document:upload', 'Upload Document', 'Documents'),
    (gen_random_uuid(), 'document:view', 'View Document', 'Documents'),
    (gen_random_uuid(), 'document:view_file', 'View Document File', 'Documents'),
    (gen_random_uuid(), 'document:edit', 'Edit Document', 'Documents'),
    (gen_random_uuid(), 'document:delete', 'Delete Document', 'Documents'),
    (gen_random_uuid(), 'document:decrypt_number', 'Decrypt Document Number', 'Documents'),
    -- User permissions
    (gen_random_uuid(), 'user:create', 'Create User', 'Users'),
    (gen_random_uuid(), 'user:view', 'View User', 'Users'),
    (gen_random_uuid(), 'user:manage', 'Manage User', 'Users'),
    -- System permissions
    (gen_random_uuid(), 'audit:view', 'View Audit Logs', 'System'),
    (gen_random_uuid(), 'permission:assign', 'Assign Permissions', 'System'),
    (gen_random_uuid(), 'setup:manage', 'Manage Setup', 'System'),
    (gen_random_uuid(), 'document_type:manage', 'Manage Document Types', 'System'),
    (gen_random_uuid(), 'client:merge', 'Merge Clients', 'Clients')
ON CONFLICT (key) DO NOTHING;
