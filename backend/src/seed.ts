import 'dotenv/config';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import { eq } from 'drizzle-orm';
import * as bcrypt from 'bcrypt';
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
  const adminPassword = 'admin123';

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

  const passwordHash = await bcrypt.hash(adminPassword, 12);

  await db
    .insert(schema.users)
    .values({
      email: adminEmail,
      passwordHash,
      fullName: 'System Admin',
      role: 'admin',
    })
    .returning();

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
  logger.log('Admin login: admin@kyc.com / admin123');

  await queryClient.end();
}

seed().catch((err) => {
  logger.error('Seed failed: ' + err.message, err.stack);
  process.exit(1);
});
