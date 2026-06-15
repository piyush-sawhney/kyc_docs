import { Injectable, Inject, BadRequestException } from '@nestjs/common';
import { eq } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { documentTypes } from '../database/schema/document-types';
import { recoveryCodes } from '../database/schema/recovery-codes';
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
    private permissionsService: PermissionsService,
  ) {}

  async needsSetup(): Promise<boolean> {
    const existing = await this.db.select({ id: users.id }).from(users).limit(1);
    return existing.length === 0;
  }

  async setup(dto: { email: string; password: string; fullName: string }) {
    const needs = await this.needsSetup();
    if (!needs) {
      throw new BadRequestException('System is already set up');
    }

    await this.permissionsService.seedDefaults();

    const passwordHash = await bcrypt.hash(dto.password, 12);

    const [admin] = await this.db
      .insert(users)
      .values({
        email: dto.email,
        passwordHash,
        fullName: dto.fullName,
        role: 'admin',
        isActive: true,
        mustChangePassword: false,
      })
      .returning();

    await this.permissionsService.assignAllToUser(admin.id);

    await this.db.insert(documentTypes).values(
      DEFAULT_DOCUMENT_TYPES.map((name) => ({ name })),
    );

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

    return {
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
