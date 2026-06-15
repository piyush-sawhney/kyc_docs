import { pgTable, uuid, varchar, text, timestamp, boolean, integer, jsonb, date } from 'drizzle-orm/pg-core';
import { clients } from './clients';
import { documentTypes } from './document-types';
import { users } from './users';

export const clientDocuments = pgTable('client_documents', {
  id: uuid('id').defaultRandom().primaryKey(),
  clientId: uuid('client_id').notNull().references(() => clients.id),
  documentTypeId: uuid('document_type_id').notNull().references(() => documentTypes.id),
  documentNumber: varchar('document_number', { length: 255 }).notNull(),
  encryptedDocumentNumber: text('encrypted_document_number'),
  side: varchar('side', { length: 10 }).notNull().default('front'),
  documentGroupId: uuid('document_group_id'),
  issueDate: date('issue_date'),
  expiryDate: date('expiry_date'),
  createdBy: uuid('created_by').references(() => users.id),
  updatedBy: uuid('updated_by').references(() => users.id),
  originalFilename: varchar('original_filename', { length: 255 }).notNull(),
  encryptedData: varchar('encrypted_data').notNull(),
  encryptionIv: varchar('encryption_iv', { length: 64 }).notNull(),
  encryptionAuthTag: varchar('encryption_auth_tag', { length: 64 }).notNull(),
  fileSize: integer('file_size'),
  mimeType: varchar('mime_type', { length: 50 }),
  metadata: jsonb('metadata'),
  isDeleted: boolean('is_deleted').notNull().default(false),
  createdAt: timestamp('created_at').notNull().defaultNow(),
  updatedAt: timestamp('updated_at').notNull().defaultNow(),
});
