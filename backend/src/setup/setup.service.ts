import { Injectable, Inject, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { eq } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as OTPLib from 'otplib';
import * as QRCode from 'qrcode';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { recoveryCodes } from '../database/schema/recovery-codes';
import { documentTypes } from '../database/schema/document-types';
import { DRIZZLE } from '../database/drizzle.provider';
import { PermissionsService } from '../permissions/permissions.service';

const DEFAULT_DOCUMENT_TYPES = [
  'PAN', 'Aadhar', 'Passport', 'Driving License', 'Voter ID', 'OCI',
];

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class SetupService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
    private jwtService: JwtService,
    private permissionsService: PermissionsService,
  ) {}

  async needsSetup(): Promise<boolean> {
    const existing = await this.db
      .select({ id: users.id })
      .from(users)
      .where(eq(users.isActive, true))
      .limit(1);
    return existing.length === 0;
  }

  async init(dto: { email: string; fullName: string }) {
    const needs = await this.needsSetup();
    if (!needs) {
      throw new BadRequestException('System is already set up');
    }

    await this.db.delete(users).where(eq(users.isActive, false));

    await this.permissionsService.seedDefaults();

    const secret = OTPLib.generateSecret();

    const [admin] = await this.db
      .insert(users)
      .values({
        email: dto.email,
        totpSecret: secret,
        fullName: dto.fullName,
        role: 'admin',
        isActive: false,
      })
      .returning();

    await this.db.insert(documentTypes).values(
      DEFAULT_DOCUMENT_TYPES.map((name) => ({ name })),
    ).onConflictDoNothing();

    const otpUri = OTPLib.generateURI({ issuer: 'KNAPS Docs', label: dto.email, secret });
    const qrDataUrl = await QRCode.toDataURL(otpUri);

    const setupToken = this.jwtService.sign({
      sub: admin.id,
      purpose: 'setup',
    }, { expiresIn: '30m' });

    return { qrDataUrl, setupToken };
  }

  async verify(setupToken: string, totpCode: string) {
    let payload: { sub: string; purpose: string };
    try {
      payload = this.jwtService.verify(setupToken);
    } catch {
      throw new BadRequestException('Invalid or expired setup token');
    }

    if (payload.purpose !== 'setup') {
      throw new BadRequestException('Invalid setup token');
    }

    const [admin] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, payload.sub))
      .limit(1);

    if (!admin) throw new BadRequestException('User not found');
    if (!admin.totpSecret) throw new BadRequestException('TOTP not initialized');

    const resultTotp = OTPLib.verifySync({ token: totpCode, secret: admin.totpSecret });
    if (!resultTotp.valid) throw new BadRequestException('Invalid verification code');

    await this.permissionsService.assignAllToUser(admin.id);

    const codes: string[] = [];
    const codeInserts: { userId: string; codeHash: string }[] = [];

    for (let i = 0; i < 5; i++) {
      const code = crypto.randomBytes(4).toString('hex').toUpperCase();
      codes.push(code);
      codeInserts.push({
        userId: admin.id,
        codeHash: await bcrypt.hash(code, 10),
      });
    }

    await this.db.insert(recoveryCodes).values(codeInserts);

    const [updated] = await this.db
      .update(users)
      .set({ isActive: true, totpVerified: true, updatedAt: new Date() })
      .where(eq(users.id, admin.id))
      .returning();

    const token = this.jwtService.sign({
      sub: admin.id,
      email: admin.email,
      role: admin.role,
    });

    return {
      token,
      user: {
        id: admin.id,
        email: admin.email,
        fullName: admin.fullName,
        role: admin.role,
      },
      recoveryCodes: codes,
    };
  }
}
