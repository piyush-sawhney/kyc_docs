import { Injectable, BadRequestException } from '@nestjs/common';
import sharp from 'sharp';

const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png'];
const ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png'];
const MAX_FILE_SIZE = 10 * 1024 * 1024;

@Injectable()
export class ImageProcessorService {
  async validate(buffer: Buffer, mimetype: string, filename: string) {
    if (!ALLOWED_MIME_TYPES.includes(mimetype)) {
      throw new BadRequestException('Only JPEG and PNG files are allowed');
    }

    const ext = '.' + filename.split('.').pop()?.toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      throw new BadRequestException('File extension must be .jpg, .jpeg, or .png');
    }

    if (buffer.length > MAX_FILE_SIZE) {
      throw new BadRequestException('File size must be less than 10MB');
    }

    const metadata = await sharp(buffer).metadata();
    if (!metadata.format) {
      throw new BadRequestException('Invalid image file');
    }
  }

  async autoOrient(buffer: Buffer): Promise<Buffer> {
    return sharp(buffer).rotate().toBuffer();
  }

  async rotate(buffer: Buffer, angle: number): Promise<Buffer> {
    return sharp(buffer).rotate(angle).toBuffer();
  }

  async crop(
    buffer: Buffer,
    options: { left: number; top: number; width: number; height: number },
  ): Promise<Buffer> {
    return sharp(buffer)
      .extract({
        left: Math.round(options.left),
        top: Math.round(options.top),
        width: Math.round(options.width),
        height: Math.round(options.height),
      })
      .toBuffer();
  }

  async optimize(buffer: Buffer, mimetype: string): Promise<{ data: Buffer; metadata: any }> {
    const inputMetadata = await sharp(buffer).metadata();

    let optimized: Buffer;
    let metadata: any = {
      originalSize: buffer.length,
      originalWidth: inputMetadata.width,
      originalHeight: inputMetadata.height,
    };

    if (mimetype === 'image/jpeg') {
      optimized = await sharp(buffer)
        .jpeg({ quality: 85, chromaSubsampling: '4:4:4', mozjpeg: true })
        .toBuffer();
    } else {
      optimized = await sharp(buffer)
        .png({ palette: true, compressionLevel: 9, quality: 85 })
        .toBuffer();
    }

    metadata.optimizedSize = optimized.length;
    metadata.reduction = `${Math.round((1 - optimized.length / buffer.length) * 100)}%`;

    return { data: optimized, metadata };
  }

  async processUpload(
    buffer: Buffer,
    mimetype: string,
    filename: string,
  ): Promise<{ processed: Buffer; metadata: any }> {
    await this.validate(buffer, mimetype, filename);

    const oriented = await this.autoOrient(buffer);

    const { data: optimized, metadata } = await this.optimize(oriented, mimetype);

    return { processed: optimized, metadata };
  }
}
