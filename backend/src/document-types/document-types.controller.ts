import { Controller, Get, Post, Put, Body, Param, UseGuards } from '@nestjs/common';
import { DocumentTypesService } from './document-types.service';
import { CreateDocumentTypeDto, UpdateDocumentTypeDto } from './dto/create-document-type.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('api/document-types')
@UseGuards(JwtAuthGuard)
export class DocumentTypesController {
  constructor(private documentTypesService: DocumentTypesService) {}

  @Get()
  findAll() {
    return this.documentTypesService.findAll();
  }

  @Post()
  @UseGuards(RolesGuard)
  @Roles('admin')
  create(@Body() dto: CreateDocumentTypeDto) {
    return this.documentTypesService.create(dto);
  }

  @Put(':id')
  @UseGuards(RolesGuard)
  @Roles('admin')
  update(@Param('id') id: string, @Body() dto: UpdateDocumentTypeDto) {
    return this.documentTypesService.update(id, dto);
  }
}
