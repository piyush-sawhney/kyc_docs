import { pgTable, uuid, varchar } from 'drizzle-orm/pg-core';

export const permissions = pgTable('permissions', {
  id: uuid('id').defaultRandom().primaryKey(),
  key: varchar('key', { length: 100 }).notNull().unique(),
  label: varchar('label', { length: 200 }).notNull(),
  group: varchar('group', { length: 100 }).notNull(),
});
