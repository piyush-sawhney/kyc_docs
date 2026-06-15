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
          await this.auditService.log({
            userId: user?.id || null,
            action,
            entityType,
            entityId: this.extractEntityId(context, request, responseBody),
            oldValues: null,
            newValues: responseBody?.data || responseBody || null,
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

  private extractEntityId(
    context: ExecutionContext,
    request: FastifyRequest,
    responseBody: any,
  ): string | null {
    const params = request.params as Record<string, string>;
    const idFromParam = params.id || params.clientId || params.documentId;
    if (idFromParam) return idFromParam;
    if (responseBody?.id) return responseBody.id;
    return null;
  }
}
