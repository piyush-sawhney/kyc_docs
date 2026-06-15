import { SetMetadata } from '@nestjs/common';

export const AUDIT_KEY = 'audit';
export const AuditAction = (metadata: {
  entityType: string;
  action: string;
}) => SetMetadata(AUDIT_KEY, metadata);
