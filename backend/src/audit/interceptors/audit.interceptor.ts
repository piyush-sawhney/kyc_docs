import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
  Logger,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Observable, mergeMap } from 'rxjs';
import { FastifyRequest } from 'fastify';
import { AuditService } from '../audit.service';
import { AUDIT_KEY } from '../decorators/audit-action.decorator';

@Injectable()
export class AuditInterceptor implements NestInterceptor {
  private readonly logger = new Logger(AuditInterceptor.name);

  constructor(
    private reflector: Reflector,
    private auditService: AuditService,
  ) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const auditMeta = this.reflector.getAllAndOverride(AUDIT_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!auditMeta) {
      return next.handle();
    }

    const request = context.switchToHttp().getRequest<FastifyRequest>();
    const user = (request as any).user;
    const { entityType, action } = auditMeta;

    return next.handle().pipe(
      mergeMap(async (responseBody) => {
        try {
          const body = responseBody?.data || responseBody || {};
          const description = this.generateDescription(entityType, action, body, request);
          await this.auditService.log({
            userId: user?.id || null,
            action,
            entityType,
            entityId: this.extractEntityId(context, request, responseBody),
            description,
            oldValues: null,
            newValues: body,
            ipAddress: request.ip,
            userAgent: request.headers['user-agent'] || null,
          });
        } catch (err) {
          this.logger.error(`Failed to log audit action ${action} on ${entityType}`, err);
        }
        return responseBody;
      }),
    );
  }

  private generateDescription(
    entityType: string,
    action: string,
    body: any,
    request: FastifyRequest,
  ): string {
    const userName = (request as any).user?.fullName || 'System';
    const url = request.url || '';
    const docType = body.documentTypeName || body.documentType?.name || 'document';
    const docNum = body.documentNumber ? `XXXXX${body.documentNumber}` : '';
    const side = body.side || 'front';

    switch (`${entityType}:${action}`) {
      case 'client:CREATE':
        return `${userName} created client '${body.name || 'Unknown'}'`;
      case 'client:UPDATE':
        return `${userName} updated client '${body.name || 'Unknown'}'`;
      case 'client:DELETE':
        return `${userName} deleted client '${body.name || 'Unknown'}'`;
      case 'document:CREATE':
        return `${userName} uploaded ${side} image for ${docType}${docNum ? ' number ' + docNum : ''}`;
      case 'document:UPDATE': {
        if (url.includes('/clear-image')) return `${userName} removed image for ${docType}`;
        if (url.includes('/metadata')) return `${userName} updated ${docType} details`;
        if (url.includes('/rotate')) return `${userName} rotated ${docType} image`;
        if (url.includes('/crop')) return `${userName} cropped ${docType} image`;
        if (url.includes('/optimize')) return `${userName} optimized ${docType} image`;
        if (request.method === 'PUT') return `${userName} re-uploaded ${docType}${docNum ? ' number ' + docNum : ''}`;
        return `${userName} updated ${docType}`;
      }
      case 'document:DELETE':
        return `${userName} deleted ${docType}${docNum ? ' number ' + docNum : ''}`;
      default:
        return `${userName} ${action.toLowerCase()} ${entityType}`;
    }
  }

  private extractEntityId(
    context: ExecutionContext,
    request: FastifyRequest,
    responseBody: any,
  ): string | null {
    const params = request.params as Record<string, string>;
    const idFromParam = params.id || params.clientId || params.documentId;
    if (idFromParam) return idFromParam;
    if (responseBody?.id) return responseBody.id;
    if (responseBody?.data?.id) return responseBody.data.id;
    return null;
  }
}
