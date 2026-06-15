import { Injectable } from '@nestjs/common';
import * as crypto from 'crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16;
const TAG_LENGTH = 16;

@Injectable()
export class EncryptionService {
  private getKey(): Buffer {
    const key = process.env.ENCRYPTION_KEY;
    if (!key) {
      throw new Error('ENCRYPTION_KEY environment variable is not set');
    }
    return Buffer.from(key, 'hex');
  }

  encrypt(buffer: Buffer): {
    encryptedData: string;
    iv: string;
    authTag: string;
  } {
    const key = this.getKey();
    const iv = crypto.randomBytes(IV_LENGTH);
    const cipher = crypto.createCipheriv(ALGORITHM, key, iv);

    const encrypted = Buffer.concat([cipher.update(buffer), cipher.final()]);
    const authTag = cipher.getAuthTag();

    return {
      encryptedData: encrypted.toString('base64'),
      iv: iv.toString('hex'),
      authTag: authTag.toString('hex'),
    };
  }

  decrypt(encryptedBase64: string, ivHex: string, authTagHex: string): Buffer {
    const key = this.getKey();
    const iv = Buffer.from(ivHex, 'hex');
    const authTag = Buffer.from(authTagHex, 'hex');
    const encrypted = Buffer.from(encryptedBase64, 'base64');

    const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(authTag);

    return Buffer.concat([decipher.update(encrypted), decipher.final()]);
  }

  encryptText(text: string): string {
    const { encryptedData, iv, authTag } = this.encrypt(Buffer.from(text, 'utf-8'));
    return `${iv}:${authTag}:${encryptedData}`;
  }

  decryptText(payload: string): string {
    const parts = payload.split(':');
    if (parts.length < 3) throw new Error('Invalid encrypted text payload');
    const iv = parts[0];
    const authTag = parts[1];
    const encryptedData = parts.slice(2).join(':');
    return this.decrypt(encryptedData, iv, authTag).toString('utf-8');
  }
}
