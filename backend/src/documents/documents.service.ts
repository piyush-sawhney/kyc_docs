import { Injectable, Inject, NotFoundException, BadRequestException } from '@nestjs/common';
import { eq, desc, and, inArray, like, not } from 'drizzle-orm';
import { PostgresJsDatabase } from 'drizzle-orm/postgres-js';
import * as crypto from 'crypto';
import * as schema from '../database/schema';
import { clientDocuments } from '../database/schema/client-documents';
import { documentTypes } from '../database/schema/document-types';
import { clients } from '../database/schema/clients';
import { users } from '../database/schema/users';
import { DRIZZLE } from '../database/drizzle.provider';
import { EncryptionService } from './services/encryption.service';
import { ImageProcessorService } from './services/image-processor.service';
import { RotateDto, CropDto, UpdateDocumentMetadataDto } from './dto/upload-document.dto';

type Database = PostgresJsDatabase<typeof schema>;

@Injectable()
export class DocumentsService {
  constructor(
    @Inject(DRIZZLE) private db: Database,
    private encryptionService: EncryptionService,
    private imageProcessor: ImageProcessorService,
  ) {}

  private encryptDocNumber(number: string): { lastFour: string; encrypted: string } {
    const lastFour = number.slice(-4).padStart(4, number.slice(-4));
    const encrypted = this.encryptionService.encryptText(number);
    return { lastFour, encrypted };
  }

  async upload(
    clientId: string,
    documentTypeId: string,
    documentNumber: string,
    side: 'front' | 'back',
    file: { buffer: Buffer; mimetype: string; filename: string },
    documentGroupId?: string,
    issueDate?: string,
    expiryDate?: string,
    userId?: string,
  ) {
    const { processed, metadata } = await this.imageProcessor.processUpload(
      file.buffer,
      file.mimetype,
      file.filename,
    );

    const { encryptedData, iv, authTag } = this.encryptionService.encrypt(processed);

    const groupId = documentGroupId || crypto.randomUUID();

    const { lastFour, encrypted: encryptedDocNumber } = this.encryptDocNumber(documentNumber);

    const [doc] = await this.db
      .insert(clientDocuments)
      .values({
        clientId,
        documentTypeId,
        documentNumber: lastFour,
        encryptedDocumentNumber: encryptedDocNumber,
        side,
        documentGroupId: groupId,
        issueDate: issueDate || null,
        expiryDate: expiryDate || null,
        createdBy: userId || null,
        updatedBy: userId || null,
        originalFilename: file.filename,
        encryptedData,
        encryptionIv: iv,
        encryptionAuthTag: authTag,
        fileSize: processed.length,
        mimeType: file.mimetype,
        metadata: JSON.stringify(metadata),
      })
      .returning();

    const [docType] = await this.db
      .select({ name: documentTypes.name })
      .from(documentTypes)
      .where(eq(documentTypes.id, documentTypeId))
      .limit(1);

    return { ...this.sanitize(doc), documentGroupId: groupId, documentTypeName: docType?.name || 'document' };
  }

  async findGroupedByClient(clientId: string) {
    const [client] = await this.db
      .select()
      .from(clients)
      .where(and(eq(clients.id, clientId), eq(clients.isDeleted, false)))
      .limit(1);

    if (!client) throw new NotFoundException('Client not found');

    const docs = await this.db
      .select()
      .from(clientDocuments)
      .where(and(
        eq(clientDocuments.clientId, clientId),
        eq(clientDocuments.isDeleted, false),
      ))
      .orderBy(desc(clientDocuments.createdAt));

    const typeIds = [...new Set(docs.map((d) => d.documentTypeId))];
    const types = typeIds.length
      ? await this.db
          .select()
          .from(documentTypes)
          .where(inArray(documentTypes.id, typeIds))
      : [];
    const typeMap = Object.fromEntries(types.map((t) => [t.id, t]));

    const groups: Record<string, any> = {};
    for (const doc of docs) {
      const gid = doc.documentGroupId || doc.id;
      if (!groups[gid]) {
        groups[gid] = {
          documentGroupId: gid,
          documentNumber: doc.documentNumber,
          encryptedDocumentNumber: doc.encryptedDocumentNumber,
          documentType: typeMap[doc.documentTypeId] || null,
          front: null,
          back: null,
          createdAt: doc.createdAt,
          updatedAt: doc.updatedAt,
        };
      }
      const sanitized = this.sanitize(doc);
      if (doc.side === 'front' || !doc.side) {
        if (!groups[gid].front) groups[gid].front = sanitized;
      } else {
        if (!groups[gid].back) groups[gid].back = sanitized;
      }
      if (doc.createdAt < groups[gid].createdAt) groups[gid].createdAt = doc.createdAt;
      if (doc.updatedAt > groups[gid].updatedAt) groups[gid].updatedAt = doc.updatedAt;
    }

    return Object.values(groups).sort(
      (a: any, b: any) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    );
  }

  async searchClients(query: string) {
    const docs = await this.db
      .select({
        clientId: clientDocuments.clientId,
        documentNumber: clientDocuments.documentNumber,
      })
      .from(clientDocuments);

    const matchingClientIds = new Set<string>();
    const lower = query.toLowerCase();
    for (const d of docs) {
      if (d.documentNumber.toLowerCase().includes(lower)) {
        matchingClientIds.add(d.clientId);
      }
    }

    return matchingClientIds;
  }

  async findOne(id: string) {
    const [doc] = await this.db
      .select()
      .from(clientDocuments)
      .where(eq(clientDocuments.id, id))
      .limit(1);

    if (!doc || doc.isDeleted) throw new NotFoundException('Document not found');
    return doc;
  }

  async findGroup(groupId: string) {
    const docs = await this.db
      .select()
      .from(clientDocuments)
      .where(and(
        eq(clientDocuments.documentGroupId, groupId),
        eq(clientDocuments.isDeleted, false),
      ));

    if (docs.length === 0) throw new NotFoundException('Document group not found');

    const [typeRow] = docs[0].documentTypeId
      ? await this.db
          .select()
          .from(documentTypes)
          .where(eq(documentTypes.id, docs[0].documentTypeId))
          .limit(1)
      : [];

    return {
      documentGroupId: groupId,
      documentNumber: docs[0].documentNumber,
      encryptedDocumentNumber: docs[0].encryptedDocumentNumber,
      documentType: typeRow || null,
      front: docs.find((d) => d.side === 'front' || !d.side) || null,
      back: docs.find((d) => d.side === 'back') || null,
      clientId: docs[0].clientId,
    };
  }

  async clearImage(id: string, userId?: string) {
    const doc = await this.findOne(id);
    const [updated] = await this.db
      .update(clientDocuments)
      .set({
        encryptedData: '',
        encryptionIv: '',
        encryptionAuthTag: '',
        fileSize: null,
        mimeType: null,
        updatedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clientDocuments.id, doc.id))
      .returning();
    return this.sanitize(updated);
  }

  async download(id: string) {
    const doc = await this.findOne(id);
    if (doc.isDeleted) throw new NotFoundException('Document has been deleted');
    if (!doc.fileSize) throw new NotFoundException('Image has been cleared');

    const decrypted = this.encryptionService.decrypt(
      doc.encryptedData,
      doc.encryptionIv,
      doc.encryptionAuthTag,
    );

    return { buffer: decrypted, mimetype: doc.mimeType, filename: doc.originalFilename };
  }

  async updateFile(id: string, file: { buffer: Buffer; mimetype: string; filename: string }, userId?: string) {
    const doc = await this.findOne(id);
    const { processed, metadata } = await this.imageProcessor.processUpload(
      file.buffer,
      file.mimetype,
      file.filename,
    );

    const { encryptedData, iv, authTag } = this.encryptionService.encrypt(processed);

    const [updated] = await this.db
      .update(clientDocuments)
      .set({
        encryptedData,
        encryptionIv: iv,
        encryptionAuthTag: authTag,
        originalFilename: file.filename,
        fileSize: processed.length,
        mimeType: file.mimetype,
        metadata: JSON.stringify(metadata),
        updatedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clientDocuments.id, id))
      .returning();

    return this.sanitize(updated);
  }

  async rotate(id: string, dto: RotateDto, userId?: string) {
    const doc = await this.findOne(id);
    const decrypted = this.encryptionService.decrypt(
      doc.encryptedData,
      doc.encryptionIv,
      doc.encryptionAuthTag,
    );

    const rotated = await this.imageProcessor.rotate(decrypted, dto.angle);
    const existingMeta = (doc.metadata as any) || {};
    const { data: optimized, metadata: optMeta } = await this.imageProcessor.optimize(rotated, doc.mimeType!);

    const { encryptedData, iv, authTag } = this.encryptionService.encrypt(optimized);

    const [updated] = await this.db
      .update(clientDocuments)
      .set({
        encryptedData,
        encryptionIv: iv,
        encryptionAuthTag: authTag,
        fileSize: optimized.length,
        metadata: JSON.stringify({ ...existingMeta, ...optMeta, rotation: dto.angle }),
        updatedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clientDocuments.id, id))
      .returning();

    return this.sanitize(updated);
  }

  async crop(id: string, dto: CropDto, userId?: string) {
    const doc = await this.findOne(id);
    const decrypted = this.encryptionService.decrypt(
      doc.encryptedData,
      doc.encryptionIv,
      doc.encryptionAuthTag,
    );

    const cropped = await this.imageProcessor.crop(decrypted, dto);
    const existingMeta = (doc.metadata as any) || {};
    const { data: optimized, metadata: optMeta } = await this.imageProcessor.optimize(cropped, doc.mimeType!);

    const { encryptedData, iv, authTag } = this.encryptionService.encrypt(optimized);

    const [updated] = await this.db
      .update(clientDocuments)
      .set({
        encryptedData,
        encryptionIv: iv,
        encryptionAuthTag: authTag,
        fileSize: optimized.length,
        metadata: JSON.stringify({ ...existingMeta, ...optMeta, crop: dto }),
        updatedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clientDocuments.id, id))
      .returning();

    return this.sanitize(updated);
  }

  async optimize(id: string, userId?: string) {
    const doc = await this.findOne(id);
    const decrypted = this.encryptionService.decrypt(
      doc.encryptedData,
      doc.encryptionIv,
      doc.encryptionAuthTag,
    );

    const { data: optimized, metadata } = await this.imageProcessor.optimize(decrypted, doc.mimeType!);
    const { encryptedData, iv, authTag } = this.encryptionService.encrypt(optimized);

    const [updated] = await this.db
      .update(clientDocuments)
      .set({
        encryptedData,
        encryptionIv: iv,
        encryptionAuthTag: authTag,
        fileSize: optimized.length,
        metadata: JSON.stringify({ ...(doc.metadata as any), ...metadata, reoptimized: true }),
        updatedBy: userId || null,
        updatedAt: new Date(),
      })
      .where(eq(clientDocuments.id, id))
      .returning();

    return this.sanitize(updated);
  }

  async updateMetadata(id: string, dto: UpdateDocumentMetadataDto, userId?: string) {
    const doc = await this.findOne(id);
    const values: Record<string, any> = { updatedBy: userId || null, updatedAt: new Date() };
    if (dto.documentTypeId !== undefined) values.documentTypeId = dto.documentTypeId;
    if (dto.documentNumber !== undefined) {
      const { lastFour, encrypted } = this.encryptDocNumber(dto.documentNumber);
      values.documentNumber = lastFour;
      values.encryptedDocumentNumber = encrypted;
    }
    if (dto.issueDate !== undefined) values.issueDate = dto.issueDate;
    if (dto.expiryDate !== undefined) values.expiryDate = dto.expiryDate;

    const [updated] = await this.db
      .update(clientDocuments)
      .set(values)
      .where(eq(clientDocuments.id, id))
      .returning();

    return this.sanitize(updated);
  }

  async softDelete(id: string, userId?: string) {
    const existing = await this.findOne(id);
    const [doc] = await this.db
      .update(clientDocuments)
      .set({ isDeleted: true, updatedBy: userId || null, updatedAt: new Date() })
      .where(eq(clientDocuments.id, existing.id))
      .returning();

    return this.sanitize(doc);
  }

  async checkNumber(documentNumber: string) {
    const allDocs = await this.db
      .select({
        id: clientDocuments.id,
        clientId: clientDocuments.clientId,
        documentNumber: clientDocuments.documentNumber,
        encryptedDocumentNumber: clientDocuments.encryptedDocumentNumber,
      })
      .from(clientDocuments)
      .where(eq(clientDocuments.isDeleted, false));

    const lower = documentNumber.toLowerCase();
    for (const doc of allDocs) {
      if (doc.documentNumber === lower.slice(-4)) {
        if (doc.encryptedDocumentNumber) {
          try {
            const decrypted = this.encryptionService.decryptText(doc.encryptedDocumentNumber);
            if (decrypted.toLowerCase() === lower) {
              const [client] = await this.db
                .select({ name: clients.name })
                .from(clients)
                .where(eq(clients.id, doc.clientId))
                .limit(1);
              return {
                exists: true,
                clientId: doc.clientId,
                clientName: client?.name || 'Unknown',
              };
            }
          } catch { /* ignore */ }
        }
      }
    }
    return { exists: false };
  }

  async decryptNumber(id: string): Promise<string> {
    const doc = await this.findOne(id);
    if (!doc.encryptedDocumentNumber) {
      throw new BadRequestException('No encrypted document number stored');
    }
    return this.encryptionService.decryptText(doc.encryptedDocumentNumber);
  }

  async getMetadata(id: string) {
    const doc = await this.findOne(id);
    let createdByUser = null;
    let updatedByUser = null;

    if (doc.createdBy) {
      const [u] = await this.db
        .select({ fullName: users.fullName, email: users.email })
        .from(users)
        .where(eq(users.id, doc.createdBy))
        .limit(1);
      createdByUser = u || null;
    }
    if (doc.updatedBy) {
      const [u] = await this.db
        .select({ fullName: users.fullName, email: users.email })
        .from(users)
        .where(eq(users.id, doc.updatedBy))
        .limit(1);
      updatedByUser = u || null;
    }

    return {
      createdAt: doc.createdAt,
      updatedAt: doc.updatedAt,
      createdBy: createdByUser,
      updatedBy: updatedByUser,
    };
  }

  private sanitize(doc: any) {
    const { encryptedData, encryptionIv, encryptionAuthTag, ...rest } = doc;
    return rest;
  }
}
