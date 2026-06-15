import { pgTable, uuid, varchar, timestamp, boolean } from 'drizzle-orm/pg-core';

export const documentTypes = pgTable('document_types', {
  id: uuid('id').defaultRandom().primaryKey(),
  name: varchar('name', { length: 100 }).notNull().unique(),
  isActive: boolean('is_active').notNull().default(true),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});
