import { Injectable, Inject } from '@nestjs/common';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import { desc, eq, and, or, ilike, SQL } from 'drizzle-orm';
import * as schema from '../database/schema';
import { auditLogs } from '../database/schema/audit-logs';
import { users } from '../database/schema/users';
import { DRIZZLE } from '../database/drizzle.provider';

type Database = PostgresJsDatabase<typeof schema>;

interface AuditEntry {
  userId: string | null;
  action: string;
  entityType: string;
  entityId?: string | null;
  description?: string | null;
  oldValues?: Record<string, any> | null;
  newValues?: Record<string, any> | null;
  ipAddress?: string | null;
  userAgent?: string | null;
}

@Injectable()
export class AuditService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async log(entry: AuditEntry) {
    const [log] = await this.db
      .insert(auditLogs)
      .values({
        userId: entry.userId,
        action: entry.action,
        entityType: entry.entityType,
        entityId: entry.entityId,
        description: entry.description,
        oldValues: entry.oldValues ? JSON.stringify(entry.oldValues) : null,
        newValues: entry.newValues ? JSON.stringify(entry.newValues) : null,
        ipAddress: entry.ipAddress,
        userAgent: entry.userAgent,
      })
      .returning();

    return log;
  }

  async findAll(params: {
    page?: number;
    limit?: number;
    entityType?: string;
    action?: string;
    userId?: string;
    entityId?: string;
    search?: string;
  }) {
    const { page = 1, limit = 50, entityType, action, userId, entityId, search } = params;
    const offset = (page - 1) * limit;

    const conditions: SQL[] = [];
    if (entityType) conditions.push(eq(auditLogs.entityType, entityType));
    if (action) conditions.push(eq(auditLogs.action, action));
    if (userId) conditions.push(eq(auditLogs.userId, userId));
    if (entityId) conditions.push(eq(auditLogs.entityId, entityId));
    if (search) conditions.push(ilike(auditLogs.description, `%${search}%`));

    const where = conditions.length > 0 ? and(...conditions) : undefined;

    const [data, totalResult] = await Promise.all([
      this.db
        .select({
          id: auditLogs.id,
          userId: auditLogs.userId,
          action: auditLogs.action,
          entityType: auditLogs.entityType,
          entityId: auditLogs.entityId,
          description: auditLogs.description,
          oldValues: auditLogs.oldValues,
          newValues: auditLogs.newValues,
          ipAddress: auditLogs.ipAddress,
          userAgent: auditLogs.userAgent,
          createdAt: auditLogs.createdAt,
          userFullName: users.fullName,
          userEmail: users.email,
        })
        .from(auditLogs)
        .leftJoin(users, eq(auditLogs.userId, users.id))
        .where(where)
        .orderBy(desc(auditLogs.createdAt))
        .limit(limit)
        .offset(offset),
      this.db
        .select({ count: auditLogs.id })
        .from(auditLogs)
        .where(where),
    ]);

    return {
      data,
      total: totalResult.length,
      page,
      limit,
    };
  }
}
