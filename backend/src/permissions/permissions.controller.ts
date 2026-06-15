import { Controller, Get, Post, Param, Body, UseGuards } from '@nestjs/common';
import { PermissionsService } from './permissions.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('api/permissions')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('admin')
export class PermissionsController {
  constructor(private permissionsService: PermissionsService) {}

  @Get()
  findAll() {
    return this.permissionsService.findAll();
  }

  @Get('user/:userId')
  getUserPermissions(@Param('userId') userId: string) {
    return this.permissionsService.getUserPermissions(userId);
  }

  @Post('user/:userId')
  setUserPermissions(
    @Param('userId') userId: string,
    @Body() body: { permissionIds: string[] },
  ) {
    return this.permissionsService.setUserPermissions(userId, body.permissionIds);
  }
}
