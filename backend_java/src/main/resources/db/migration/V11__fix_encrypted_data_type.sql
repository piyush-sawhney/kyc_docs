ALTER TABLE client_documents ALTER COLUMN encrypted_data TYPE BYTEA USING encrypted_data::bytea;
