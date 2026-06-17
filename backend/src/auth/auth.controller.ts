import { Controller, Post, Get, Body, Res, UseGuards, HttpCode } from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { AuthService } from './auth.service';
import { LoginDto, ChangePasswordDto, RecoveryLoginDto, RecoveryResetPasswordDto } from './dto/login.dto';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { RolesGuard } from './guards/roles.guard';
import { Roles } from './decorators/roles.decorator';
import { Public } from './decorators/public.decorator';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@Controller('api/auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  private setCookie(reply: FastifyReply, token: string) {
    reply.setCookie('token', token, {
      httpOnly: true,
      secure: false,
      sameSite: 'strict',
      path: '/',
      maxAge: 86400,
    });
  }

  @Public()
  @Post('login')
  async login(@Body() dto: LoginDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.authService.login(dto.email, dto.password);
    this.setCookie(reply, result.token);
    return { mustChangePassword: result.mustChangePassword, user: result.user };
  }

  @Public()
  @Post('recovery/reset-password')
  async recoveryResetPassword(@Body() dto: RecoveryResetPasswordDto) {
    return this.authService.recoveryResetPassword(dto.email, dto.recoveryCode, dto.newPassword);
  }

  @Public()
  @Post('logout')
  @HttpCode(200)
  async logout(@Res({ passthrough: true }) reply: FastifyReply) {
    reply.clearCookie('token', { path: '/' });
    return { message: 'Logged out' };
  }

  @Public()
  @Post('recovery')
  async recoveryLogin(@Body() dto: RecoveryLoginDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.authService.loginWithRecovery(dto.email, dto.code);
    this.setCookie(reply, result.token);
    return { mustChangePassword: result.mustChangePassword, user: result.user };
  }

  @Post('change-password')
  @UseGuards(JwtAuthGuard)
  async changePassword(@CurrentUser() user: any, @Body() dto: ChangePasswordDto) {
    return this.authService.changePassword(user.id, dto.currentPassword, dto.newPassword);
  }

  @Get('recovery-codes')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  async getRecoveryCodes(@CurrentUser() user: any) {
    return this.authService.getRecoveryCodes(user.id);
  }

  @Get('recovery-codes/status')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  async getRecoveryCodesStatus(@CurrentUser() user: any) {
    const hasUnusedCodes = await this.authService.checkRecoveryCodesStatus(user.id);
    return { hasUnusedCodes };
  }

  @Post('recovery-codes')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  async generateRecoveryCodes(@CurrentUser() user: any) {
    return this.authService.generateRecoveryCodes(user.id);
  }

  @Get('me')
  @UseGuards(JwtAuthGuard)
  async getProfile(@CurrentUser() user: any) {
    return this.authService.getProfile(user.id);
  }
}
