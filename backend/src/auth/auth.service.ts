import { Injectable, Inject, UnauthorizedException, BadRequestException, ForbiddenException, NotFoundException, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { eq, and } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import * as OTPLib from 'otplib';
import * as QRCode from 'qrcode';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { recoveryCodes } from '../database/schema/recovery-codes';
import { DRIZZLE } from '../database/drizzle.provider';
import { AuditService } from '../audit/audit.service';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    @Inject(DRIZZLE) private db: Database,
    private jwtService: JwtService,
    private auditService: AuditService,
  ) {}

  private generateToken(user: { id: string; email: string; role: string }) {
    return this.jwtService.sign({
      sub: user.id,
      email: user.email,
      role: user.role,
    });
  }

  async loginInit(email: string) {
    const [user] = await this.db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
        isDeleted: users.isDeleted,
        totpSecret: users.totpSecret,
        totpVerified: users.totpVerified,
      })
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (!user) throw new UnauthorizedException('Invalid credentials');
    if (user.isDeleted) throw new UnauthorizedException('Account has been removed');
    if (!user.isActive) throw new UnauthorizedException('Account is deactivated');

    if (user.totpSecret && user.totpVerified) {
      return { enrolled: true };
    }

    const secret = OTPLib.generateSecret();
    const otpUri = OTPLib.generateURI({ issuer: 'KNAPS Docs', label: user.email, secret });
    const qrDataUrl = await QRCode.toDataURL(otpUri);

    await this.db
      .update(users)
      .set({ totpSecret: secret, updatedAt: new Date() })
      .where(eq(users.id, user.id));

    const enrollToken = this.jwtService.sign({
      sub: user.id,
      purpose: 'enroll',
    }, { expiresIn: '15m' });

    return { enrolled: false, enrollToken, qrDataUrl };
  }

  async login(email: string, totpCode: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (!user) throw new UnauthorizedException('Invalid credentials');
    if (user.isDeleted) throw new UnauthorizedException('Account has been removed');
    if (!user.isActive) throw new UnauthorizedException('Account is deactivated');
    if (!user.totpSecret || !user.totpVerified) throw new UnauthorizedException('TOTP not enrolled');

    const result = OTPLib.verifySync({ token: totpCode, secret: user.totpSecret });
    if (!result.valid) throw new UnauthorizedException('Invalid verification code');

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
    }).catch((err) => this.logger.error('Failed to log login audit', err));

    return {
      token,
      recoveryCodesMissing: user.role === 'admin' ? !(await this.checkRecoveryCodesStatus(user.id)) : false,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
      },
    };
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
          recoveryCodesMissing: user.role === 'admin' ? !(await this.checkRecoveryCodesStatus(user.id)) : false,
          user: { id: user.id, email: user.email, fullName: user.fullName, role: user.role },
        };
      }
    }

    throw new UnauthorizedException('Invalid recovery code');
  }

  async totpEnroll(enrollToken: string, totpCode: string) {
    let payload: { sub: string; purpose: string };
    try {
      payload = this.jwtService.verify(enrollToken);
    } catch {
      throw new UnauthorizedException('Invalid or expired enrollment token');
    }

    if (payload.purpose !== 'enroll') {
      throw new UnauthorizedException('Invalid enrollment token');
    }

    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, payload.sub))
      .limit(1);

    if (!user) throw new UnauthorizedException('User not found');
    if (!user.totpSecret) throw new UnauthorizedException('TOTP secret not initialized');

    const resultTotp = OTPLib.verifySync({ token: totpCode, secret: user.totpSecret });
    if (!resultTotp.valid) throw new UnauthorizedException('Invalid verification code');

    await this.db
      .update(users)
      .set({ isActive: true, totpVerified: true, updatedAt: new Date() })
      .where(eq(users.id, user.id));

    const token = this.generateToken(user);

    const recoveryResult = await this.generateRecoveryCodesInternal(user.id);

    this.auditService.log({
      userId: user.id,
      action: 'TOTP_ENROLL',
      entityType: 'auth',
      entityId: user.id,
      description: `${user.fullName} enrolled TOTP authenticator`,
      newValues: { email: user.email },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log TOTP enrollment audit', err));

    return {
      token,
      user: { id: user.id, email: user.email, fullName: user.fullName, role: user.role },
      recoveryCodes: user.role === 'admin' ? recoveryResult : undefined,
    };
  }

  async getEnrollQr(enrollToken: string) {
    let payload: { sub: string; purpose: string };
    try {
      payload = this.jwtService.verify(enrollToken);
    } catch {
      throw new UnauthorizedException('Invalid or expired enrollment token');
    }

    if (payload.purpose !== 'enroll') {
      throw new UnauthorizedException('Invalid enrollment token');
    }

    const [user] = await this.db
      .select({ email: users.email, totpSecret: users.totpSecret })
      .from(users)
      .where(eq(users.id, payload.sub))
      .limit(1);

    if (!user) throw new UnauthorizedException('User not found');
    if (!user.totpSecret) throw new UnauthorizedException('TOTP not initialized');

    const otpUri = OTPLib.generateURI({ issuer: 'KNAPS Docs', label: user.email, secret: user.totpSecret });
    const qrDataUrl = await QRCode.toDataURL(otpUri);

    return { qrDataUrl };
  }

  async reEnroll(userId: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    if (!user) throw new UnauthorizedException('User not found');

    const secret = OTPLib.generateSecret();
    const otpUri = OTPLib.generateURI({ issuer: 'KNAPS Docs', label: user.email, secret });
    const qrDataUrl = await QRCode.toDataURL(otpUri);

    await this.db
      .update(users)
      .set({ totpSecret: secret, updatedAt: new Date() })
      .where(eq(users.id, userId));

    return { qrDataUrl };
  }

  async adminReEnroll(userId: string, adminId?: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    if (!user) throw new NotFoundException('User not found');

    await this.db
      .update(users)
      .set({ totpSecret: null, totpVerified: false, updatedAt: new Date() })
      .where(eq(users.id, userId));

    this.auditService.log({
      userId: adminId || null,
      action: 'TOTP_REENROLL',
      entityType: 'user',
      entityId: userId,
      description: `Admin reset authenticator for user '${user.fullName}' (${user.email})`,
      newValues: { email: user.email },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log TOTP admin re-enrollment audit', err));

    return { message: 'Authenticator re-enrollment initiated. User will be prompted to set up on next login.' };
  }

  async reEnrollVerify(userId: string, totpCode: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    if (!user) throw new UnauthorizedException('User not found');
    if (!user.totpSecret) throw new BadRequestException('No TOTP secret found. Initiate re-enrollment first.');

    const resultTotp = OTPLib.verifySync({ token: totpCode, secret: user.totpSecret });
    if (!resultTotp.valid) throw new BadRequestException('Invalid verification code');

    await this.db
      .update(users)
      .set({ totpVerified: true, updatedAt: new Date() })
      .where(eq(users.id, userId));

    this.auditService.log({
      userId,
      action: 'TOTP_REENROLL',
      entityType: 'auth',
      entityId: userId,
      description: `${user.fullName} re-enrolled TOTP authenticator`,
      newValues: { email: user.email },
      ipAddress: null,
      userAgent: null,
    }).catch((err) => this.logger.error('Failed to log TOTP re-enrollment audit', err));

    return { message: 'Authenticator re-enrolled successfully' };
  }

  private async generateRecoveryCodesInternal(userId: string): Promise<string[]> {
    const newCodes: string[] = [];
    const inserts: { userId: string; codeHash: string }[] = [];

    for (let i = 0; i < 5; i++) {
      const code = crypto.randomBytes(4).toString('hex').toUpperCase();
      newCodes.push(code);
      inserts.push({ userId, codeHash: await bcrypt.hash(code, 10) });
    }

    await this.db.insert(recoveryCodes).values(inserts);
    return newCodes;
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
    const [targetUser] = await this.db
      .select({ role: users.role })
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);
    if (!targetUser || targetUser.role !== 'admin') {
      throw new ForbiddenException('Recovery codes are only available for admin users');
    }

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
    }).catch((err) => this.logger.error('Failed to log recovery codes audit', err));

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
        createdAt: users.createdAt,
      })
      .from(users)
      .where(eq(users.id, userId))
      .limit(1);

    return user;
  }
}
