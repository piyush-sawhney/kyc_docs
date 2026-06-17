import { Controller, Get, Post, Body, Res } from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { IsEmail, IsString } from 'class-validator';
import { Public } from '../auth/decorators/public.decorator';
import { SetupService } from './setup.service';

class SetupInitDto {
  @IsEmail()
  email: string;

  @IsString()
  fullName: string;
}

class SetupVerifyDto {
  @IsString()
  setupToken: string;

  @IsString()
  totpCode: string;
}

@Controller('api/setup')
export class SetupController {
  constructor(private setupService: SetupService) {}

  @Public()
  @Get('status')
  async status() {
    const needsSetup = await this.setupService.needsSetup();
    return { needsSetup };
  }

  @Public()
  @Post('init')
  async init(@Body() dto: SetupInitDto) {
    return this.setupService.init(dto);
  }

  @Public()
  @Post('verify')
  async verify(@Body() dto: SetupVerifyDto, @Res({ passthrough: true }) reply: FastifyReply) {
    const result = await this.setupService.verify(dto.setupToken, dto.totpCode);
    reply.setCookie('token', result.token, {
      httpOnly: true,
      secure: false,
      sameSite: 'strict',
      path: '/',
      maxAge: 86400,
    });
    return { user: result.user, recoveryCodes: result.recoveryCodes };
  }
}
