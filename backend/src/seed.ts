import 'dotenv/config';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import { eq } from 'drizzle-orm';
import * as OTPLib from 'otplib';
import * as QRCode from 'qrcode';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { Logger } from '@nestjs/common';
import * as schema from './database/schema';
import 'dotenv/config'

const logger = new Logger('Seed');

async function seed() {
  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) {
    logger.error('DATABASE_URL environment variable is required');
    process.exit(1);
  }

  const queryClient = postgres(databaseUrl);
  const db = drizzle({ client: queryClient, schema });

  const adminEmail = 'admin@kyc.com';

  const [existing] = await db
    .select()
    .from(schema.users)
    .where(eq(schema.users.email, adminEmail))
    .limit(1);

  if (existing) {
    logger.log('Admin user already exists. Skipping seed.');
    await queryClient.end();
    return;
  }

  const secret = OTPLib.generateSecret();

  const [admin] = await db
    .insert(schema.users)
    .values({
      email: adminEmail,
      totpSecret: secret,
      totpVerified: true,
      fullName: 'System Admin',
      role: 'admin',
    })
    .returning();

  const qrDataUrl = await QRCode.toDataURL(OTPLib.generateURI({ issuer: 'KNAPS Docs', label: adminEmail, secret }));

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

  await db.insert(schema.recoveryCodes).values(codeInserts);

  logger.log('Admin user created: ' + adminEmail);

  const defaultDocTypes = ['PAN Card', 'Aadhaar Card', 'Passport', 'Driving License', 'Voter ID'];
  for (const name of defaultDocTypes) {
    await db
      .insert(schema.documentTypes)
      .values({ name })
      .returning();
    logger.log('Document type created: ' + name);
  }

  logger.log('Seed complete!');
  logger.log('Admin login: admin@kyc.com');
  logger.log('Scan this QR code with Google Authenticator:');
  logger.log(qrDataUrl);

  await queryClient.end();
}

seed().catch((err) => {
  logger.error('Seed failed: ' + err.message, err.stack);
  process.exit(1);
});
