import { Module } from '@nestjs/common';
import { DocumentsController } from './documents.controller';
import { DocumentsService } from './documents.service';
import { EncryptionService } from './services/encryption.service';
import { ImageProcessorService } from './services/image-processor.service';
import { PermissionsModule } from '../permissions/permissions.module';

@Module({
  imports: [PermissionsModule],
  controllers: [DocumentsController],
  providers: [DocumentsService, EncryptionService, ImageProcessorService],
  exports: [DocumentsService],
})
export class DocumentsModule {}
