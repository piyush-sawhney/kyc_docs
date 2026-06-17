package com.kycdocs.domain.document;

import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.documenttype.DocumentTypeId;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.shared.SoftDeletableEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Document extends SoftDeletableEntity {

    private ClientId clientId;
    private DocumentTypeId documentTypeId;
    private String documentNumber;
    private String encryptedDocumentNumber;
    private DocumentSide side;
    private DocumentGroupId documentGroupId;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private UserId createdBy;
    private UserId updatedBy;
    private String originalFilename;
    private EncryptedData encryptedData;
    private int fileSize;
    private String mimeType;
    private String metadata;

    public Document(UUID id, Instant createdAt, Instant updatedAt,
                    ClientId clientId, DocumentTypeId documentTypeId,
                    String documentNumber, DocumentSide side,
                    String originalFilename, EncryptedData encryptedData,
                    int fileSize, String mimeType, UserId createdBy) {
        super(id, createdAt, updatedAt);
        this.clientId = clientId;
        this.documentTypeId = documentTypeId;
        this.documentNumber = documentNumber;
        this.side = side;
        this.originalFilename = originalFilename;
        this.encryptedData = encryptedData;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.createdBy = createdBy;
    }

    public ClientId getClientId() { return clientId; }
    public DocumentTypeId getDocumentTypeId() { return documentTypeId; }
    public String getDocumentNumber() { return documentNumber; }
    public String getEncryptedDocumentNumber() { return encryptedDocumentNumber; }
    public DocumentSide getSide() { return side; }
    public DocumentGroupId getDocumentGroupId() { return documentGroupId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public UserId getCreatedBy() { return createdBy; }
    public UserId getUpdatedBy() { return updatedBy; }
    public String getOriginalFilename() { return originalFilename; }
    public EncryptedData getEncryptedData() { return encryptedData; }
    public int getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public String getMetadata() { return metadata; }

    public void updateEncryptedDocumentNumber(String encryptedNumber) {
        this.encryptedDocumentNumber = encryptedNumber;
    }

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void updateEncryptedData(EncryptedData encryptedData) {
        this.encryptedData = encryptedData;
    }

    public void updateFileInfo(String originalFilename, int fileSize, String mimeType) {
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public void setUpdatedBy(UserId updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void assignToGroup(DocumentGroupId groupId) {
        this.documentGroupId = groupId;
    }

    public void updateType(DocumentTypeId documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public void updateDates(LocalDate issueDate, LocalDate expiryDate) {
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
    }

    public void clearImageData() {
        this.encryptedData = null;
        this.fileSize = 0;
        this.mimeType = null;
        this.metadata = null;
    }
}
