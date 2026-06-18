CREATE TABLE IF NOT EXISTS client_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id),
    document_type_id UUID NOT NULL REFERENCES document_types(id),
    document_number VARCHAR(255) NOT NULL,
    encrypted_document_number TEXT,
    side VARCHAR(10) NOT NULL DEFAULT 'front',
    document_group_id UUID,
    issue_date DATE,
    expiry_date DATE,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    encrypted_data BYTEA NOT NULL,
    encryption_iv VARCHAR(64) NOT NULL,
    encryption_auth_tag VARCHAR(64) NOT NULL,
    file_size INTEGER,
    mime_type VARCHAR(50),
    metadata JSONB,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_client_documents_client_id ON client_documents(client_id);
CREATE INDEX IF NOT EXISTS idx_client_documents_document_type_id ON client_documents(document_type_id);
CREATE INDEX IF NOT EXISTS idx_client_documents_document_group_id ON client_documents(document_group_id);
CREATE INDEX IF NOT EXISTS idx_client_documents_is_deleted ON client_documents(is_deleted);
