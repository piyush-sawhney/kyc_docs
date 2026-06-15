import { Controller, Get, Query, UseGuards, BadRequestException } from '@nestjs/common';
import { AuditService } from './audit.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@Controller('api/audit-logs')
@UseGuards(JwtAuthGuard)
export class AuditController {
  constructor(private auditService: AuditService) {}

  @Get()
  findAll(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('entityType') entityType?: string,
    @Query('action') action?: string,
    @Query('userId') userId?: string,
    @Query('entityId') entityId?: string,
    @CurrentUser() user?: any,
  ) {
    if (user?.role !== 'admin' && !entityId) {
      throw new BadRequestException('entityId is required for non-admin users');
    }
    return this.auditService.findAll({
      page: page ? parseInt(page, 10) : 1,
      limit: limit ? parseInt(limit, 10) : 50,
      entityType,
      action,
      userId,
      entityId,
    });
  }
}
