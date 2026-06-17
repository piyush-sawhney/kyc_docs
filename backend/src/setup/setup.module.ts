import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { SetupController } from './setup.controller';
import { SetupService } from './setup.service';
import { PermissionsModule } from '../permissions/permissions.module';

@Module({
  imports: [
    PermissionsModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        secret: config.get('JWT_SECRET'),
        signOptions: { expiresIn: config.get('JWT_EXPIRATION', '24h') },
      }),
      inject: [ConfigService],
    }),
  ],
  controllers: [SetupController],
  providers: [SetupService],
})
export class SetupModule {}
