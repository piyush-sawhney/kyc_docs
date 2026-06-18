CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(200) NOT NULL,
    "group" VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_permissions_key ON permissions(key);
