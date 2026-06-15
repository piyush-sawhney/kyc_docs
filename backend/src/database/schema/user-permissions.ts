import { pgTable, uuid, primaryKey } from 'drizzle-orm/pg-core';
import { users } from './users';
import { permissions } from './permissions';

export const userPermissions = pgTable('user_permissions', {
  userId: uuid('user_id').notNull().references(() => users.id),
  permissionId: uuid('permission_id').notNull().references(() => permissions.id),
}, (t) => ({
  pk: primaryKey({ columns: [t.userId, t.permissionId] }),
}));
