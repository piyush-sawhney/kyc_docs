import 'dotenv/config';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import { eq } from 'drizzle-orm';
import * as bcrypt from 'bcrypt';
import * as schema from './database/schema';
import 'dotenv/config'
async function seed() {
  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) {
    console.error('DATABASE_URL environment variable is required');
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
    console.log('Admin user already exists. Skipping seed.');
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

  console.log('Admin user created:', adminEmail);

  const defaultDocTypes = ['PAN Card', 'Aadhaar Card', 'Passport', 'Driving License', 'Voter ID'];
  for (const name of defaultDocTypes) {
    await db
      .insert(schema.documentTypes)
      .values({ name })
      .returning();
    console.log('Document type created:', name);
  }

  console.log('\nSeed complete!');
  console.log('Admin login: admin@kyc.com / admin123');

  await queryClient.end();
}

seed().catch((err) => {
  console.error('Seed failed:', err);
  process.exit(1);
});
