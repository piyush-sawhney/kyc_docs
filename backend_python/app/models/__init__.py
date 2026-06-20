from sqlmodel import SQLModel

from app.models.audit_log import AuditLog as AuditLog
from app.models.document_type import DocumentType as DocumentType
from app.models.permission import Permission as Permission
from app.models.recovery_code import RecoveryCode as RecoveryCode
from app.models.user import User as User
from app.models.user_permission import UserPermission as UserPermission

Base = SQLModel
