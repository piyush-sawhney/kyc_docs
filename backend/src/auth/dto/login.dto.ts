import { IsEmail, IsString, MinLength } from 'class-validator';

export class LoginInitDto {
  @IsEmail()
  email: string;
}

export class TotpLoginDto {
  @IsEmail()
  email: string;

  @IsString()
  totpCode: string;
}

export class TotpEnrollDto {
  @IsString()
  enrollToken: string;

  @IsString()
  totpCode: string;
}

export class RecoveryLoginDto {
  @IsEmail()
  email: string;

  @IsString()
  code: string;
}
