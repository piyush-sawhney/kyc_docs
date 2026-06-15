import { IsString, IsOptional, IsNumber } from 'class-validator';

export class UploadDocumentDto {
  @IsString()
  documentTypeId: string;

  @IsString()
  documentNumber: string;
}

export class UpdateDocumentMetadataDto {
  @IsOptional()
  @IsString()
  documentTypeId?: string;

  @IsOptional()
  @IsString()
  documentNumber?: string;

  @IsOptional()
  @IsString()
  issueDate?: string;

  @IsOptional()
  @IsString()
  expiryDate?: string;
}

export class RotateDto {
  @IsNumber()
  angle: number;
}

export class CropDto {
  @IsNumber()
  left: number;

  @IsNumber()
  top: number;

  @IsNumber()
  width: number;

  @IsNumber()
  height: number;
}
