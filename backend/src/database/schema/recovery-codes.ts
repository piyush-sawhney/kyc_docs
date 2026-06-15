import { pgTable, uuid, varchar, boolean, timestamp } from 'drizzle-orm/pg-core';
import { users } from './users';

export const recoveryCodes = pgTable('recovery_codes', {
  id: uuid('id').defaultRandom().primaryKey(),
  userId: uuid('user_id').notNull().references(() => users.id),
  codeHash: varchar('code_hash', { length: 255 }).notNull(),
  isUsed: boolean('is_used').notNull().default(false),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});
