import { Controller, Get, Post, Put, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { ClientsService } from './clients.service';
import { CreateClientDto } from './dto/create-client.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { PermissionsGuard } from '../permissions/guards/permissions.guard';
import { RequirePermissions } from '../permissions/decorators/permissions.decorator';
import { CurrentUser } from '../common/decorators/current-user.decorator';
import { AuditAction } from '../audit/decorators/audit-action.decorator';

@Controller('api/clients')
@UseGuards(JwtAuthGuard, PermissionsGuard)
export class ClientsController {
  constructor(private clientsService: ClientsService) {}

  @Get()
  @RequirePermissions('client:read')
  findAll() {
    return this.clientsService.findAll();
  }

  @Get('deleted')
  @UseGuards(RolesGuard)
  @Roles('admin')
  findDeleted() {
    return this.clientsService.findDeleted();
  }

  @Post()
  @RequirePermissions('client:create')
  @AuditAction({ entityType: 'client', action: 'CREATE' })
  create(@Body() dto: CreateClientDto, @CurrentUser() user: any) {
    return this.clientsService.create(dto, user.id);
  }

  @Get(':id')
  @RequirePermissions('client:read')
  findOne(@Param('id') id: string) {
    return this.clientsService.findOneWithDocuments(id);
  }

  @Put(':id')
  @RequirePermissions('client:update')
  @AuditAction({ entityType: 'client', action: 'UPDATE' })
  update(@Param('id') id: string, @Body() dto: CreateClientDto) {
    return this.clientsService.update(id, dto);
  }

  @Patch(':id')
  @RequirePermissions('client:update')
  patch(@Param('id') id: string, @Body('name') name?: string, @Body('avatar') avatar?: string) {
    return this.clientsService.update(id, { name, avatar });
  }

  @Delete(':id')
  @RequirePermissions('client:delete')
  @AuditAction({ entityType: 'client', action: 'DELETE' })
  remove(@Param('id') id: string, @CurrentUser() user: any) {
    return this.clientsService.remove(id, user.id);
  }

  @Post(':id/restore')
  @UseGuards(RolesGuard)
  @Roles('admin')
  @AuditAction({ entityType: 'client', action: 'UPDATE' })
  restore(@Param('id') id: string) {
    return this.clientsService.restore(id);
  }

  @Post(':id/merge')
  @RequirePermissions('client:update')
  @AuditAction({ entityType: 'client', action: 'UPDATE' })
  merge(@Param('id') id: string, @Body('targetClientId') targetClientId: string, @CurrentUser() user: any) {
    return this.clientsService.merge(id, targetClientId, user.id);
  }
}
