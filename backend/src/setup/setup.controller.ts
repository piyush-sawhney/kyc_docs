import { Controller, Get, Post, Body } from '@nestjs/common';
import { IsEmail, IsString, MinLength } from 'class-validator';
import { SetupService } from './setup.service';

class SetupDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(6)
  password: string;

  @IsString()
  fullName: string;
}

@Controller('api/setup')
export class SetupController {
  constructor(private setupService: SetupService) {}

  @Get('status')
  async status() {
    const needsSetup = await this.setupService.needsSetup();
    return { needsSetup };
  }

  @Post()
  async setup(@Body() dto: SetupDto) {
    return this.setupService.setup(dto);
  }
}
