import { Controller, Get, Post, Put, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { ClientsService } from './clients.service';
import { CreateClientDto } from './dto/create-client.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { AuditAction } from '../audit/decorators/audit-action.decorator';

@Controller('api/clients')
@UseGuards(JwtAuthGuard)
export class ClientsController {
  constructor(private clientsService: ClientsService) {}

  @Get()
  findAll() {
    return this.clientsService.findAll();
  }

  @Post()
  @AuditAction({ entityType: 'client', action: 'CREATE' })
  create(@Body() dto: CreateClientDto, @CurrentUser() user: any) {
    return this.clientsService.create(dto, user.id);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.clientsService.findOneWithDocuments(id);
  }

  @Put(':id')
  @AuditAction({ entityType: 'client', action: 'UPDATE' })
  update(@Param('id') id: string, @Body() dto: CreateClientDto) {
    return this.clientsService.update(id, dto);
  }

  @Patch(':id')
  patch(@Param('id') id: string, @Body('name') name?: string, @Body('avatar') avatar?: string) {
    return this.clientsService.update(id, { name, avatar });
  }

  @Delete(':id')
  @AuditAction({ entityType: 'client', action: 'DELETE' })
  remove(@Param('id') id: string) {
    return this.clientsService.remove(id);
  }

  @Post(':id/merge')
  @AuditAction({ entityType: 'client', action: 'UPDATE' })
  merge(@Param('id') id: string, @Body('targetClientId') targetClientId: string) {
    return this.clientsService.merge(id, targetClientId);
  }
}
