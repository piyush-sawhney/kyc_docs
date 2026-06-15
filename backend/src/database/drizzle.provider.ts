import { FactoryProvider } from '@nestjs/common';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import * as schema from './schema';

export const DRIZZLE = 'DRIZZLE' as const;

export const drizzleProvider: FactoryProvider = {
  provide: DRIZZLE,
  useFactory: () => {
    const queryClient = postgres(process.env.DATABASE_URL!);
    return drizzle({ client: queryClient, schema });
  },
};
