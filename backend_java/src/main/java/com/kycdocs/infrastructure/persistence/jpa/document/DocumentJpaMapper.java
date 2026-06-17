package com.kycdocs.infrastructure.persistence.jpa.document;

import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.document.*;
import com.kycdocs.domain.documenttype.DocumentTypeId;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class DocumentJpaMapper {

    public ClientDocumentJpaEntity toJpa(Document domain) {
        if (domain == null) return null;
        var entity = new ClientDocumentJpaEntity();
        entity.setId(domain.getId());
        entity.setClientId(domain.getClientId().value());
        entity.setDocumentTypeId(domain.getDocumentTypeId().value());
        entity.setDocumentNumber(domain.getDocumentNumber());
        entity.setEncryptedDocumentNumber(domain.getEncryptedDocumentNumber());
        entity.setSide(domain.getSide().getValue());
        entity.setDocumentGroupId(domain.getDocumentGroupId() != null ? domain.getDocumentGroupId().value() : null);
        entity.setIssueDate(domain.getIssueDate());
        entity.setExpiryDate(domain.getExpiryDate());
        entity.setCreatedBy(domain.getCreatedBy() != null ? domain.getCreatedBy().value() : null);
        entity.setUpdatedBy(domain.getUpdatedBy() != null ? domain.getUpdatedBy().value() : null);
        entity.setOriginalFilename(domain.getOriginalFilename());
        if (domain.getEncryptedData() != null) {
            entity.setEncryptedData(domain.getEncryptedData().ciphertext());
            entity.setEncryptionIv(domain.getEncryptedData().ivBase64());
            entity.setEncryptionAuthTag(domain.getEncryptedData().authTagBase64());
        }
        entity.setFileSize(domain.getFileSize());
        entity.setMimeType(domain.getMimeType());
        entity.setMetadata(domain.getMetadata());
        entity.setDeleted(domain.isDeleted());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public Document toDomain(ClientDocumentJpaEntity entity) {
        if (entity == null) return null;
        var encryptedData = new EncryptedData(
            entity.getEncryptedData(),
            Base64.getDecoder().decode(entity.getEncryptionIv()),
            Base64.getDecoder().decode(entity.getEncryptionAuthTag())
        );
        var doc = new Document(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            new ClientId(entity.getClientId()),
            new DocumentTypeId(entity.getDocumentTypeId()),
            entity.getDocumentNumber(),
            DocumentSide.fromString(entity.getSide()),
            entity.getOriginalFilename(),
            encryptedData,
            entity.getFileSize() != null ? entity.getFileSize() : 0,
            entity.getMimeType(),
            entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null
        );
        if (entity.getEncryptedDocumentNumber() != null) {
            doc.updateEncryptedDocumentNumber(entity.getEncryptedDocumentNumber());
        }
        if (entity.getDocumentGroupId() != null) {
            doc.assignToGroup(new DocumentGroupId(entity.getDocumentGroupId()));
        }
        if (entity.getIssueDate() != null || entity.getExpiryDate() != null) {
            doc.updateDates(entity.getIssueDate(), entity.getExpiryDate());
        }
        if (entity.getUpdatedBy() != null) {
            doc.setUpdatedBy(new UserId(entity.getUpdatedBy()));
        }
        if (entity.getMetadata() != null) {
            doc.updateMetadata(entity.getMetadata());
        }
        if (entity.isDeleted()) {
            doc.softDelete(null);
        }
        return doc;
    }
}
