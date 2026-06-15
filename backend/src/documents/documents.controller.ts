import {
  Controller, Get, Post, Put, Patch, Delete, Body, Param, UseGuards, Req, Res, Query
} from '@nestjs/common';
import { FastifyReply, FastifyRequest } from 'fastify';
import { DocumentsService } from './documents.service';
import { RotateDto, CropDto, UpdateDocumentMetadataDto } from './dto/upload-document.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { AuditAction } from '../audit/decorators/audit-action.decorator';
import { extractMultipart } from './multipart.util';

@Controller('api')
@UseGuards(JwtAuthGuard)
export class DocumentsController {
  constructor(private documentsService: DocumentsService) {}

  @Post('clients/:clientId/documents')
  @AuditAction({ entityType: 'document', action: 'CREATE' })
  async upload(@Param('clientId') clientId: string, @Req() req: FastifyRequest, @CurrentUser() user: any) {
    const { file, fields } = await extractMultipart(req);
    return this.documentsService.upload(
      clientId,
      fields.documentTypeId,
      fields.documentNumber,
      (fields.side as 'front' | 'back') || 'front',
      file,
      fields.documentGroupId,
      fields.issueDate,
      fields.expiryDate,
      user.id,
    );
  }

  @Get('clients/:clientId/documents/grouped')
  findGroupedByClient(@Param('clientId') clientId: string) {
    return this.documentsService.findGroupedByClient(clientId);
  }

  @Get('clients/:clientId/documents')
  findFlatByClient(@Param('clientId') clientId: string) {
    return this.documentsService.findGroupedByClient(clientId);
  }

  @Get('documents/group/:groupId')
  findGroup(@Param('groupId') groupId: string) {
    return this.documentsService.findGroup(groupId);
  }

  @Get('documents/:id/download')
  async download(@Param('id') id: string, @Res() reply: FastifyReply) {
    const { buffer, mimetype, filename } = await this.documentsService.download(id);
    reply.header('Content-Type', mimetype);
    reply.header('Content-Disposition', `attachment; filename="${filename}"`);
    reply.send(buffer);
  }

  @Put('documents/:id')
  @AuditAction({ entityType: 'document', action: 'UPDATE' })
  async updateFile(@Param('id') id: string, @Req() req: FastifyRequest, @CurrentUser() user: any) {
    const { file } = await extractMultipart(req);
    return this.documentsService.updateFile(id, file, user.id);
  }

  @Patch('documents/:id/metadata')
  @AuditAction({ entityType: 'document', action: 'UPDATE' })
  updateMetadata(@Param('id') id: string, @Body() dto: UpdateDocumentMetadataDto, @CurrentUser() user: any) {
    return this.documentsService.updateMetadata(id, dto, user.id);
  }

  @Delete('documents/:id')
  @AuditAction({ entityType: 'document', action: 'DELETE' })
  softDelete(@Param('id') id: string, @CurrentUser() user: any) {
    return this.documentsService.softDelete(id, user.id);
  }

  @Post('documents/:id/rotate')
  @AuditAction({ entityType: 'document', action: 'UPDATE' })
  rotate(@Param('id') id: string, @Body() dto: RotateDto, @CurrentUser() user: any) {
    return this.documentsService.rotate(id, dto, user.id);
  }

  @Post('documents/:id/crop')
  @AuditAction({ entityType: 'document', action: 'UPDATE' })
  crop(@Param('id') id: string, @Body() dto: CropDto, @CurrentUser() user: any) {
    return this.documentsService.crop(id, dto, user.id);
  }

  @Post('documents/:id/optimize')
  @AuditAction({ entityType: 'document', action: 'UPDATE' })
  optimize(@Param('id') id: string, @CurrentUser() user: any) {
    return this.documentsService.optimize(id, user.id);
  }

  @Get('documents/check-number/:number')
  checkNumber(@Param('number') number: string) {
    return this.documentsService.checkNumber(number);
  }

  @Get('documents/:id/metadata')
  getMetadata(@Param('id') id: string) {
    return this.documentsService.getMetadata(id);
  }

  @Get('documents/:id/decrypt-number')
  decryptNumber(@Param('id') id: string) {
    return this.documentsService.decryptNumber(id);
  }
}
