import { IsString, MinLength, IsOptional } from 'class-validator';

export class CreateDocumentTypeDto {
  @IsString()
  @MinLength(1)
  name: string;
}

export class UpdateDocumentTypeDto {
  @IsOptional()
  @IsString()
  name?: string;
}
