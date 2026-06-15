import { Injectable, Inject } from '@nestjs/common';
import { eq, inArray } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as schema from '../database/schema';
import { permissions } from '../database/schema/permissions';
import { userPermissions } from '../database/schema/user-permissions';
import { DRIZZLE } from '../database/drizzle.provider';

type Database = PostgresJsDatabase<typeof schema>;

export const DEFAULT_PERMISSION_KEYS: { key: string; label: string; group: string }[] = [
  { key: 'client:create', label: 'Create Clients', group: 'Clients' },
  { key: 'client:read', label: 'View Clients', group: 'Clients' },
  { key: 'client:update', label: 'Edit Clients', group: 'Clients' },
  { key: 'client:delete', label: 'Delete Clients', group: 'Clients' },
  { key: 'document:upload', label: 'Upload Documents', group: 'Documents' },
  { key: 'document:view', label: 'View Documents', group: 'Documents' },
  { key: 'document:view_number', label: 'View Document Numbers', group: 'Documents' },
  { key: 'document:view_file', label: 'Open Document File', group: 'Documents' },
  { key: 'document:update', label: 'Replace Document File', group: 'Documents' },
  { key: 'document:edit_image', label: 'Edit Images (Rotate/Crop/Optimize)', group: 'Documents' },
  { key: 'document:delete', label: 'Delete Documents', group: 'Documents' },
  { key: 'user:view', label: 'View Users', group: 'Users' },
  { key: 'user:create', label: 'Create Users', group: 'Users' },
  { key: 'user:manage', label: 'Edit / Activate Users', group: 'Users' },
  { key: 'user:reset_password', label: 'Reset User Passwords', group: 'Users' },
  { key: 'audit:view', label: 'View Audit Logs', group: 'System' },
  { key: 'permission:manage', label: 'Manage Permissions', group: 'System' },
];

@Injectable()
export class PermissionsService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async seedDefaults() {
    const existing = await this.db.select().from(permissions).limit(1);
    if (existing.length > 0) return;

    await this.db.insert(permissions).values(DEFAULT_PERMISSION_KEYS);
  }

  async findAll() {
    return this.db.select().from(permissions).orderBy(permissions.group, permissions.key);
  }

  async getUserPermissions(userId: string) {
    const rows = await this.db
      .select({ permissionId: permissions.id, key: permissions.key, label: permissions.label, group: permissions.group })
      .from(userPermissions)
      .innerJoin(permissions, eq(userPermissions.permissionId, permissions.id))
      .where(eq(userPermissions.userId, userId));

    return rows;
  }

  async setUserPermissions(userId: string, permissionIds: string[]) {
    await this.db.delete(userPermissions).where(eq(userPermissions.userId, userId));

    if (permissionIds.length > 0) {
      await this.db.insert(userPermissions).values(
        permissionIds.map((permissionId) => ({ userId, permissionId })),
      );
    }
  }

  async assignAllToUser(userId: string) {
    const all = await this.db.select({ id: permissions.id }).from(permissions);
    await this.setUserPermissions(userId, all.map((p) => p.id));
  }
}
