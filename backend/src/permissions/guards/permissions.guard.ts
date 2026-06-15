import { Injectable, CanActivate, ExecutionContext, Inject } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { eq, inArray } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as schema from '../../database/schema';
import { userPermissions } from '../../database/schema/user-permissions';
import { permissions } from '../../database/schema/permissions';
import { DRIZZLE } from '../../database/drizzle.provider';
import { PERMISSIONS_KEY } from '../decorators/permissions.decorator';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class PermissionsGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const requiredPermissions = this.reflector.getAllAndOverride<string[]>(
      PERMISSIONS_KEY,
      [context.getHandler(), context.getClass()],
    );

    if (!requiredPermissions || requiredPermissions.length === 0) {
      return true;
    }

    const { user } = context.switchToHttp().getRequest();
    if (!user) return false;

    if (user.role === 'admin') return true;

    const userPermRows = await this.db
      .select({ key: permissions.key })
      .from(userPermissions)
      .innerJoin(permissions, eq(userPermissions.permissionId, permissions.id))
      .where(eq(userPermissions.userId, user.id));

    const userKeys = userPermRows.map((r) => r.key);
    return requiredPermissions.every((key) => userKeys.includes(key));
  }
}
