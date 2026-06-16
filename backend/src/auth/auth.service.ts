import { Injectable, Inject, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { eq, and } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { recoveryCodes } from '../database/schema/recovery-codes';
import { DRIZZLE } from '../database/drizzle.provider';
import { AuditService } from '../audit/audit.service';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class AuthService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
    private jwtService: JwtService,
    private auditService: AuditService,
  ) {}

  private async validateUser(email: string, password: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (!user) throw new UnauthorizedException('Invalid credentials');
    if (!user.isActive) throw new UnauthorizedException('Account is deactivated');
    if (user.isDeleted) throw new UnauthorizedException('Account has been removed');

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) throw new UnauthorizedException('Invalid credentials');

    return user;
  }

  private generateToken(user: { id: string; email: string; role: string }) {
    return this.jwtService.sign({
      sub: user.id,
      email: user.email,
      role: user.role,
    });
  }

  async login(email: string, password: string) {
    const user = await this.validateUser(email, password);

    const token = this.generateToken(user);

    this.auditService.log({
      userId: user.id,
      action: 'LOGIN',
      entityType: 'auth',
      entityId: user.id,
      description: `${user.fullName} logged in`,
      newValues: { email: user.email },
      ipAddress: null,
      userAgent: null,
    }).catch(() => {});

    return {
      token,
      mustChangePassword: user.mustChangePassword,
      recoveryCodesMissing: user.role === 'admin' ? !(await this.checkRecoveryCodesStatus(user.id)) : false,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
      },
    };
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    if (!user) throw new UnauthorizedException();

    const valid = await bcrypt.compare(currentPassword, user.passwordHash);
    if (!valid) throw new BadRequestException('Current password is incorrect');

    const passwordHash = await bcrypt.hash(newPassword, 12);
    await this.db
      .update(users)
      .set({ passwordHash, mustChangePassword: false, updatedAt: new Date() })
      .where(eq(users.id, userId));

    this.auditService.log({
      userId,
      action: 'PASSWORD_CHANGE',
      entityType: 'auth',
      entityId: userId,
      description: `${user.fullName} changed their password`,
      newValues: { userId },
      ipAddress: null,
      userAgent: null,
    }).catch(() => {});

    return { message: 'Password changed' };
  }

  async loginWithRecovery(email: string, code: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (!user) throw new UnauthorizedException('Invalid credentials');
    if (!user.isActive) throw new UnauthorizedException('Account is deactivated');
    if (user.isDeleted) throw new UnauthorizedException('Account has been removed');

    const allCodes = await this.db
      .select()
      .from(recoveryCodes)
      .where(and(eq(recoveryCodes.userId, user.id), eq(recoveryCodes.isUsed, false)));

    for (const rc of allCodes) {
      const match = await bcrypt.compare(code.trim().toUpperCase(), rc.codeHash);
      if (match) {
        await this.db
          .update(recoveryCodes)
          .set({ isUsed: true })
          .where(eq(recoveryCodes.id, rc.id));

        const token = this.generateToken(user);
        return {
          token,
          mustChangePassword: user.mustChangePassword,
          recoveryCodesMissing: user.role === 'admin' ? !(await this.checkRecoveryCodesStatus(user.id)) : false,
          user: { id: user.id, email: user.email, fullName: user.fullName, role: user.role },
        };
      }
    }

    throw new UnauthorizedException('Invalid recovery code');
  }

  async getRecoveryCodes(userId: string) {
    const codes = await this.db
      .select({ id: recoveryCodes.id, isUsed: recoveryCodes.isUsed, createdAt: recoveryCodes.createdAt })
      .from(recoveryCodes)
      .where(eq(recoveryCodes.userId, userId))
      .orderBy(recoveryCodes.createdAt);

    return codes;
  }

  async generateRecoveryCodes(userId: string) {
    await this.db.delete(recoveryCodes).where(eq(recoveryCodes.userId, userId));

    const newCodes: string[] = [];
    const inserts: { userId: string; codeHash: string }[] = [];

    for (let i = 0; i < 5; i++) {
      const code = crypto.randomBytes(4).toString('hex').toUpperCase();
      newCodes.push(code);
      inserts.push({ userId, codeHash: await bcrypt.hash(code, 10) });
    }

    await this.db.insert(recoveryCodes).values(inserts);

    this.auditService.log({
      userId,
      action: 'RECOVERY_CODES',
      entityType: 'auth',
      entityId: userId,
      description: `${(await this.getProfile(userId))?.fullName || 'User'} generated new recovery codes`,
      newValues: { userId },
      ipAddress: null,
      userAgent: null,
    }).catch(() => {});

    return { recoveryCodes: newCodes };
  }

  async checkRecoveryCodesStatus(userId: string): Promise<boolean> {
    const [user] = await this.db
      .select({ role: users.role })
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    if (!user || user.role !== 'admin') return true;

    const unused = await this.db
      .select({ id: recoveryCodes.id })
      .from(recoveryCodes)
      .where(and(eq(recoveryCodes.userId, userId), eq(recoveryCodes.isUsed, false)))
      .limit(1);

    return unused.length > 0;
  }

  async getProfile(userId: string) {
    const [user] = await this.db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      })
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    return user;
  }
}
