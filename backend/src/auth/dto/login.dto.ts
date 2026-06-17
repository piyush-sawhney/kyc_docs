import { IsEmail, IsString, MinLength, IsOptional } from 'class-validator';
import { IsStrongPassword } from '../../common/decorators/password.decorator';

export class LoginDto {
  @IsEmail()
  email: string;

  @IsString()
  password: string;
}

export class ChangePasswordDto {
  @IsString()
  currentPassword: string;

  @IsString()
  @IsStrongPassword()
  newPassword: string;
}

export class RecoveryLoginDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(6)
  code: string;
}

export class RecoveryResetPasswordDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(6)
  recoveryCode: string;

  @IsString()
  @IsStrongPassword()
  newPassword: string;
}
