import { Injectable, Inject, NotFoundException, BadRequestException } from '@nestjs/common';
import { eq, desc, and, SQL } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as schema from '../database/schema';
import { clients } from '../database/schema/clients';
import { clientDocuments } from '../database/schema/client-documents';
import { DRIZZLE } from '../database/drizzle.provider';
import { CreateClientDto } from './dto/create-client.dto';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class ClientsService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async create(dto: CreateClientDto, userId: string) {
    const [client] = await this.db
      .insert(clients)
      .values({
        name: dto.name,
        createdBy: userId,
      })
      .returning();

    return client;
  }

  async findAll() {
    return this.db
      .select()
      .from(clients)
      .where(eq(clients.isDeleted, false))
      .orderBy(desc(clients.createdAt));
  }

  async findDeleted() {
    return this.db
      .select()
      .from(clients)
      .where(eq(clients.isDeleted, true))
      .orderBy(desc(clients.deletedAt));
  }

  async findOne(id: string) {
    const [client] = await this.db
      .select()
      .from(clients)
      .where(eq(clients.id, id))
      .limit(1);

    if (!client || client.isDeleted) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async findOneWithDocuments(id: string) {
    const client = await this.findOne(id);

    const docs = await this.db
      .select()
      .from(clientDocuments)
      .where(and(
        eq(clientDocuments.clientId, id),
        eq(clientDocuments.isDeleted, false),
      ))
      .orderBy(desc(clientDocuments.createdAt));

    return { ...client, documents: docs };
  }

  async update(id: string, data: { name?: string; avatar?: string }) {
    const [client] = await this.db
      .update(clients)
      .set({ ...data, updatedAt: new Date() })
      .where(and(eq(clients.id, id), eq(clients.isDeleted, false)))
      .returning();

    if (!client) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async remove(id: string, userId?: string) {
    const [client] = await this.db
      .update(clients)
      .set({
        isDeleted: true,
        deletedAt: new Date(),
        deletedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(and(eq(clients.id, id), eq(clients.isDeleted, false)))
      .returning();

    if (!client) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async restore(id: string) {
    const [client] = await this.db
      .update(clients)
      .set({
        isDeleted: false,
        deletedAt: null,
        deletedBy: null,
        updatedAt: new Date(),
      })
      .where(and(eq(clients.id, id), eq(clients.isDeleted, true)))
      .returning();

    if (!client) {
      throw new NotFoundException('Deleted client not found');
    }
    return client;
  }

  async merge(sourceId: string, targetId: string, userId?: string) {
    const source = await this.db
      .select()
      .from(clients)
      .where(eq(clients.id, sourceId))
      .limit(1);

    if (!source[0] || source[0].isDeleted) {
      throw new NotFoundException('Source client not found');
    }

    const target = await this.db
      .select()
      .from(clients)
      .where(eq(clients.id, targetId))
      .limit(1);

    if (!target[0] || target[0].isDeleted) {
      throw new NotFoundException('Target client not found');
    }

    await this.db
      .update(clientDocuments)
      .set({ clientId: targetId })
      .where(eq(clientDocuments.clientId, sourceId));

    await this.db
      .update(clients)
      .set({
        isDeleted: true,
        deletedAt: new Date(),
        deletedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clients.id, sourceId));

    return this.findOne(targetId);
  }
}
