import { IsString, MinLength, IsOptional } from 'class-validator';

export class CreateClientDto {
  @IsString()
  @MinLength(1)
  name: string;

  @IsOptional()
  @IsString()
  avatar?: string;
}
