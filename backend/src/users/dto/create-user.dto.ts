import { IsEmail, IsString, IsOptional, IsIn } from 'class-validator';
import { IsStrongPassword } from '../../common/decorators/password.decorator';

export class CreateUserDto {
  @IsEmail()
  email: string;

  @IsString()
  @IsStrongPassword()
  password: string;

  @IsString()
  fullName: string;

  @IsOptional()
  @IsIn(['admin', 'user'])
  role?: string;
}
