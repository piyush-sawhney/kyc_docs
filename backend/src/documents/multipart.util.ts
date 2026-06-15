import { BadRequestException } from '@nestjs/common';
import { FastifyRequest } from 'fastify';

export interface UploadedFile {
  buffer: Buffer;
  mimetype: string;
  filename: string;
}

export interface MultipartParsed {
  file: UploadedFile;
  fields: Record<string, string>;
}

export async function extractMultipart(req: FastifyRequest): Promise<MultipartParsed> {
  const file = await req.file();

  if (!file) {
    throw new BadRequestException('No file uploaded');
  }

  const chunks: Buffer[] = [];
  for await (const chunk of file.file) {
    chunks.push(chunk);
  }
  const buffer = Buffer.concat(chunks);

  const fields: Record<string, string> = {};
  if (file.fields) {
    for (const [key, value] of Object.entries(file.fields)) {
      fields[key] = (value as any).value !== undefined
        ? String((value as any).value)
        : String(value);
    }
  }

  return {
    file: {
      buffer,
      mimetype: file.mimetype,
      filename: file.filename,
    },
    fields,
  };
}
