import { Injectable, Inject, NotFoundException, ConflictException, BadRequestException } from '@nestjs/common';
import { eq } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as bcrypt from 'bcrypt';
import * as schema from '../database/schema';
import { users } from '../database/schema/users';
import { DRIZZLE } from '../database/drizzle.provider';
import { CreateUserDto } from './dto/create-user.dto';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class UsersService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async create(dto: CreateUserDto) {
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
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      });

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
        mustChangePassword: users.mustChangePassword,
        createdAt: users.createdAt,
      })
      .from(users)
      .orderBy(users.createdAt);
  }

  async findOne(id: string) {
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
      .where(eq(users.id, id))
      .limit(1);

    if (!user) {
      throw new NotFoundException('User not found');
    }
    return user;
  }

  async deactivate(id: string) {
    const [user] = await this.db
      .update(users)
      .set({ isActive: false, updatedAt: new Date() })
      .where(eq(users.id, id))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
      });

    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async reactivate(id: string) {
    const [user] = await this.db
      .update(users)
      .set({ isActive: true, updatedAt: new Date() })
      .where(eq(users.id, id))
      .returning({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        isActive: users.isActive,
      });

    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async resetPassword(id: string) {
    const [user] = await this.db
      .select()
      .from(users)
      .where(eq(users.id, id))
      .limit(1);

    if (!user) throw new NotFoundException('User not found');

    const tempPassword = 'Temp@' + Math.random().toString(36).slice(-6);
    const passwordHash = await bcrypt.hash(tempPassword, 12);

    await this.db
      .update(users)
      .set({ passwordHash, mustChangePassword: true, updatedAt: new Date() })
      .where(eq(users.id, id));

    return { tempPassword };
  }
}
