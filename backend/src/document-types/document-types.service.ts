import { Injectable, Inject, NotFoundException, ConflictException } from '@nestjs/common';
import { eq, inArray } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as schema from '../database/schema';
import { documentTypes } from '../database/schema/document-types';
import { DRIZZLE } from '../database/drizzle.provider';
import { CreateDocumentTypeDto, UpdateDocumentTypeDto } from './dto/create-document-type.dto';

const DEFAULT_DOCUMENT_TYPES = [
  'PAN', 'Aadhar', 'Passport', 'Driving License', 'Voter ID', 'OCI',
];

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class DocumentTypesService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
  ) {}

  async create(dto: CreateDocumentTypeDto) {
    const [existing] = await this.db
      .select()
      .from(documentTypes)
      .where(eq(documentTypes.name, dto.name))
      .limit(1);

    if (existing) {
      throw new ConflictException('Document type already exists');
    }

    const [dt] = await this.db
      .insert(documentTypes)
      .values({ name: dto.name })
      .returning();

    return dt;
  }

  async findAll() {
    const existing = await this.db
      .select({ name: documentTypes.name })
      .from(documentTypes);

    const missing = DEFAULT_DOCUMENT_TYPES.filter(
      (name) => !existing.some((e) => e.name === name),
    );

    if (missing.length > 0) {
      await this.db.insert(documentTypes).values(missing.map((name) => ({ name })));
    }

    return this.db
      .select()
      .from(documentTypes)
      .orderBy(documentTypes.name);
  }

  async findOne(id: string) {
    const [dt] = await this.db
      .select()
      .from(documentTypes)
      .where(eq(documentTypes.id, id))
      .limit(1);

    if (!dt) {
      throw new NotFoundException('Document type not found');
    }
    return dt;
  }

  async update(id: string, dto: UpdateDocumentTypeDto) {
    const [dt] = await this.db
      .update(documentTypes)
      .set(dto)
      .where(eq(documentTypes.id, id))
      .returning();

    if (!dt) {
      throw new NotFoundException('Document type not found');
    }
    return dt;
  }
}
