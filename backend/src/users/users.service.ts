import { Injectable, Inject, NotFoundException, ConflictException, BadRequestException, ForbiddenException, Logger } from '@nestjs/common';
import { eq, and } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as bcrypt from 'bcrypt';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { DRIZZLE } from '../database/drizzle.provider';
import { CreateUserDto } from './dto/create-user.dto';
import { AuditService } from '../audit/audit.service';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class UsersService {
  private readonly logger = new Logger(UsersService.name);

  constructor(
    @Inject(DRIZZLE) private db: Database,
    private auditService: AuditService,
  ) {}

  async create(dto: CreateUserDto, currentUserId?: string) {
    const [existing] = await this.db
      .select()
      .from(users)
      .where(eq(users.email, dto.email))
      .limit(1);

    if (existing) {
      throw new ConflictException('Email already in use');
    }

    const passwordHash = await bcrypt.hash(dto.password, 12);

    const [user] = await this.db
      .insert(users)
      .values({
        email: dto.email,
        passwordHash,
        fullName: dto.fullName,
        role: dto.role || 'user',
        mustChangePassword: true,
      })
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      });

    this.auditService.log({
      userId: currentUserId || null,
      action: 'CREATE',
      entityType: 'user',
      entityId: user.id,
      description: `Created user '${user.fullName}' (${user.email})`,
      newValues: { id: user.id, email: user.email, fullName: user.fullName, role: user.role },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return user;
  }

  async findAll() {
    return this.db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      })
      .from(users)
      .where(eq(users.isDeleted, false))
      .orderBy(users.createdAt);
  }

  async findDeleted() {
    return this.db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
        deletedAt: users.deletedAt,
      })
      .from(users)
      .where(eq(users.isDeleted, true))
      .orderBy(users.deletedAt);
  }

  async findOne(id: string) {
    const [user] = await this.db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      })
      .from(users)
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .limit(1);

    if (!user) {
      throw new NotFoundException('User not found');
    }
    return user;
  }

  async deactivate(id: string, currentUserId?: string) {
    const [target] = await this.db
      .select({ id: users.id, role: users.role })
      .from(users)
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .limit(1);

    if (!target) throw new NotFoundException('User not found');

    const activeUsers = await this.db
      .select({ id: users.id })
      .from(users)
      .where(and(eq(users.isDeleted, false), eq(users.isActive, true)));

    if (activeUsers.length <= 1) {
      throw new ForbiddenException('Cannot deactivate the only active user. At least 2 active users are required in the system.');
    }

    if (target.role === 'admin') {
      const activeAdmins = await this.db
        .select({ id: users.id })
        .from(users)
        .where(and(eq(users.isDeleted, false), eq(users.isActive, true), eq(users.role, 'admin')));

      if (activeAdmins.length <= 1) {
        throw new ForbiddenException('Cannot deactivate the last active admin. At least 1 admin must remain active.');
      }
    }

    const [user] = await this.db
      .update(users)
      .set({ isActive: false, updatedAt: new Date() })
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
      });

    if (!user) throw new NotFoundException('User not found');

    this.auditService.log({
      userId: currentUserId || null,
      action: 'DEACTIVATE',
      entityType: 'user',
      entityId: id,
      description: `Deactivated user '${user.fullName}'`,
      newValues: { id, fullName: user.fullName },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return user;
  }

  async reactivate(id: string, currentUserId?: string) {
    const [user] = await this.db
      .update(users)
      .set({ isActive: true, updatedAt: new Date() })
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
      });

    if (!user) throw new NotFoundException('User not found');

    this.auditService.log({
      userId: currentUserId || null,
      action: 'REACTIVATE',
      entityType: 'user',
      entityId: id,
      description: `Reactivated user '${user.fullName}'`,
      newValues: { id, fullName: user.fullName },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return user;
  }

  async softDelete(id: string, currentUserId?: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .limit(1);

    if (!user) throw new NotFoundException('User not found');

    const [deleted] = await this.db
      .update(users)
      .set({
        isDeleted: true,
        deletedAt: new Date(),
        isActive: false,
        updatedAt: new Date(),
      })
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
      });

    this.auditService.log({
      userId: currentUserId || null,
      action: 'DELETE',
      entityType: 'user',
      entityId: id,
      description: `Deleted user '${user.fullName}' (${user.email})`,
      newValues: { id, email: user.email, fullName: user.fullName },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return deleted;
  }

  async restore(id: string, currentUserId?: string) {
    const [user] = await this.db
      .update(users)
      .set({
        isDeleted: false,
        deletedAt: null,
        isActive: true,
        updatedAt: new Date(),
      })
      .where(and(eq(users.id, id), eq(users.isDeleted, true)))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
      });

    if (!user) throw new NotFoundException('Deleted user not found');

    this.auditService.log({
      userId: currentUserId || null,
      action: 'RESTORE',
      entityType: 'user',
      entityId: id,
      description: `Restored user '${user.fullName}' (${user.email})`,
      newValues: { id, email: user.email, fullName: user.fullName },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return user;
  }

  async updateRole(id: string, role: 'admin' | 'user', currentUserId?: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .limit(1);

    if (!user) throw new NotFoundException('User not found');

    const oldRole = user.role;

    if (oldRole === 'admin' && role === 'user') {
      const adminCount = await this.db
        .select({ count: users.id })
        .from(users)
        .where(and(eq(users.role, 'admin'), eq(users.isDeleted, false), eq(users.isActive, true)));

      if (adminCount.length <= 1) {
        throw new ForbiddenException('Cannot demote the last admin. Promote another user to admin first.');
      }
    }

    const [updated] = await this.db
      .update(users)
      .set({ role, updatedAt: new Date() })
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
      });

    this.auditService.log({
      userId: currentUserId || null,
      action: 'ROLE_CHANGE',
      entityType: 'user',
      entityId: id,
      description: `Changed role of '${user.fullName}' from ${oldRole} to ${role}`,
      newValues: { id, email: user.email, fullName: user.fullName, oldRole, newRole: role },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return updated;
  }

  async resetPassword(id: string, currentUserId?: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(and(eq(users.id, id), eq(users.isDeleted, false)))
      .limit(1);

    if (!user) throw new NotFoundException('User not found');

    const tempPassword = 'Temp@' + Math.random().toString(36).slice(-6);
    const passwordHash = await bcrypt.hash(tempPassword, 12);

    await this.db
      .update(users)
      .set({ passwordHash, mustChangePassword: true, updatedAt: new Date() })
      .where(eq(users.id, id));

    this.auditService.log({
      userId: currentUserId || null,
      action: 'RESET_PASSWORD',
      entityType: 'user',
      entityId: id,
      description: `Reset password for user '${user.fullName}' (${user.email})`,
      newValues: { id, email: user.email, fullName: user.fullName },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log audit for user operation', err));

    return { tempPassword };
  }
}
