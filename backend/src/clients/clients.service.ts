import { Injectable, Inject, NotFoundException } from '@nestjs/common';
import { eq, desc } from 'drizzle-orm';
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
      .orderBy(desc(clients.createdAt));
  }

  async findOne(id: string) {
    const [client] = await this.db
      .select()
      .from(clients)
      .where(eq(clients.id, id))
      .limit(1);

    if (!client) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async findOneWithDocuments(id: string) {
    const client = await this.findOne(id);

    const docs = await this.db
      .select()
      .from(clientDocuments)
      .where(eq(clientDocuments.clientId, id))
      .orderBy(desc(clientDocuments.createdAt));

    return { ...client, documents: docs };
  }

  async update(id: string, data: { name?: string; avatar?: string }) {
    const [client] = await this.db
      .update(clients)
      .set({ ...data, updatedAt: new Date() })
      .where(eq(clients.id, id))
      .returning();

    if (!client) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async remove(id: string) {
    await this.db
      .delete(clientDocuments)
      .where(eq(clientDocuments.clientId, id));

    const [client] = await this.db
      .delete(clients)
      .where(eq(clients.id, id))
      .returning();

    if (!client) {
      throw new NotFoundException('Client not found');
    }
    return client;
  }

  async merge(sourceId: string, targetId: string) {
    await this.db
      .update(clientDocuments)
      .set({ clientId: targetId })
      .where(eq(clientDocuments.clientId, sourceId));

    const [source] = await this.db
      .delete(clients)
      .where(eq(clients.id, sourceId))
      .returning();

    if (!source) throw new NotFoundException('Source client not found');

    return this.findOne(targetId);
  }
}
