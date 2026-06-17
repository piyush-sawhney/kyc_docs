import { Controller, Post, Get, Body, Param, Res, UseGuards, HttpCode, Query } from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { AuthService } from './auth.service';
import { LoginInitDto, TotpLoginDto, TotpEnrollDto, RecoveryLoginDto } from './dto/login.dto';
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
  @Post('login-init')
  async loginInit(@Body() dto: LoginInitDto) {
    return this.authService.loginInit(dto.email);
  }

  @Public()
  @Post('login')
  async login(@Body() dto: TotpLoginDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.authService.login(dto.email, dto.totpCode);
    this.setCookie(reply, result.token);
    return { recoveryCodesMissing: result.recoveryCodesMissing, user: result.user };
  }

  @Public()
  @Post('recovery')
  async recoveryLogin(@Body() dto: RecoveryLoginDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.authService.loginWithRecovery(dto.email, dto.code);
    this.setCookie(reply, result.token);
    return { recoveryCodesMissing: result.recoveryCodesMissing, user: result.user };
  }

  @Public()
  @Get('totp/enroll-qr')
  async getEnrollQr(@Query('token') enrollToken: string) {
    return this.authService.getEnrollQr(enrollToken);
  }

  @Public()
  @Post('totp/enroll')
  async totpEnroll(@Body() dto: TotpEnrollDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.authService.totpEnroll(dto.enrollToken, dto.totpCode);
    this.setCookie(reply, result.token);
    return {
      user: result.user,
      recoveryCodes: result.recoveryCodes,
    };
  }

  @Post('totp/re-enroll')
  @UseGuards(JwtAuthGuard)
  async reEnroll(@CurrentUser() user: any) {
    return this.authService.reEnroll(user.id);
  }

  @Post('totp/re-enroll/verify')
  @UseGuards(JwtAuthGuard)
  async reEnrollVerify(@CurrentUser() user: any, @Body('totpCode') totpCode: string) {
    return this.authService.reEnrollVerify(user.id, totpCode);
  }

  @Public()
  @Post('logout')
  @HttpCode(200)
  async logout(@Res({ passthrough: true }) reply: FastifyReply) {
    reply.clearCookie('token', { path: '/' });
    return { message: 'Logged out' };
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
