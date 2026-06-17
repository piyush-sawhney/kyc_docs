import { Controller, Get, Post, Patch, Body, Param, UseGuards } from '@nestjs/common';
import { UsersService } from './users.service';
import { AuthService } from '../auth/auth.service';
import { CreateUserDto } from './dto/create-user.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { PermissionsGuard } from '../permissions/guards/permissions.guard';
import { RequirePermissions } from '../permissions/decorators/permissions.decorator';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@Controller('api/users')
@UseGuards(JwtAuthGuard, RolesGuard, PermissionsGuard)
export class UsersController {
  constructor(
    private usersService: UsersService,
    private authService: AuthService,
  ) {}

  @Get()
  @RequirePermissions('user:view')
  findAll() {
    return this.usersService.findAll();
  }

  @Get('deleted')
  @RequirePermissions('user:view')
  findDeleted() {
    return this.usersService.findDeleted();
  }

  @Post()
  @RequirePermissions('user:create')
  create(@Body() dto: CreateUserDto, @CurrentUser() user: any) {
    return this.usersService.create(dto, user?.id);
  }

  @Get(':id')
  @RequirePermissions('user:view')
  findOne(@Param('id') id: string) {
    return this.usersService.findOne(id);
  }

  @Post(':id/deactivate')
  @RequirePermissions('user:manage')
  deactivate(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.deactivate(id, user?.id);
  }

  @Post(':id/reactivate')
  @RequirePermissions('user:manage')
  reactivate(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.reactivate(id, user?.id);
  }

  @Post(':id/soft-delete')
  @RequirePermissions('user:manage')
  softDelete(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.softDelete(id, user?.id);
  }

  @Post(':id/restore')
  @RequirePermissions('user:manage')
  restore(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.restore(id, user?.id);
  }

  @Patch(':id/role')
  @RequirePermissions('user:manage')
  updateRole(@Param('id') id: string, @Body('role') role: 'admin' | 'user', @CurrentUser() user: any) {
    return this.usersService.updateRole(id, role, user?.id);
  }

  @Post(':id/reset-password')
  @RequirePermissions('user:reset_password')
  resetPassword(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.resetPassword(id, user?.id);
  }

  @Post(':id/recovery-codes')
  @Roles('admin')
  generateRecoveryCodes(@Param('id') id: string, @CurrentUser() user: any) {
    return this.authService.generateRecoveryCodes(id);
  }
}
