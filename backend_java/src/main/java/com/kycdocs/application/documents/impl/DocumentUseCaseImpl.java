package com.kycdocs.application.documents.impl;

import com.kycdocs.application.documents.DocumentUseCase;
import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.document.*;
import com.kycdocs.domain.documenttype.DocumentTypeId;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.service.document.DocumentEncryptionService;
import com.kycdocs.service.imaging.ImageProcessingService;
import com.kycdocs.shared.exception.NotFoundException;
import com.kycdocs.shared.exception.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class DocumentUseCaseImpl implements DocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentEncryptionService encryptionService;
    private final ImageProcessingService imageProcessingService;

    public DocumentUseCaseImpl(DocumentRepository documentRepository,
                               DocumentEncryptionService encryptionService,
                               ImageProcessingService imageProcessingService) {
        this.documentRepository = documentRepository;
        this.encryptionService = encryptionService;
        this.imageProcessingService = imageProcessingService;
    }

    @Override
    public Document upload(String clientId, String documentTypeId, String documentNumber, String side,
                           byte[] fileData, String originalFilename, String mimeType, int fileSize,
                           String documentGroupId, String issueDate, String expiryDate, String userId) {
        var processResult = imageProcessingService.processUpload(fileData, mimeType, originalFilename);
        var encrypted = encryptionService.encryptDocument(processResult.data());
        var lastFour = documentNumber.length() >= 4
            ? documentNumber.substring(documentNumber.length() - 4)
            : documentNumber;
        var encryptedNumber = encryptionService.encryptDocumentNumber(documentNumber);

        var doc = new Document(
            UUID.randomUUID(), Instant.now(), Instant.now(),
            ClientId.fromString(clientId),
            DocumentTypeId.fromString(documentTypeId),
            lastFour,
            DocumentSide.fromString(side),
            originalFilename,
            encrypted,
            processResult.data().length,
            mimeType,
            userId != null ? UserId.fromString(userId) : null
        );
        doc.updateEncryptedDocumentNumber(encryptedNumber);
        doc.updateMetadata(processResult.metadata().toString());

        if (documentGroupId != null) {
            doc.assignToGroup(DocumentGroupId.fromString(documentGroupId));
        }
        if (issueDate != null) {
            doc.updateDates(LocalDate.parse(issueDate),
                expiryDate != null ? LocalDate.parse(expiryDate) : null);
        }

        return documentRepository.save(doc);
    }

    @Override
    public List<Map<String, Object>> findGroupedByClient(String clientId) {
        var docs = documentRepository.findByClientId(ClientId.fromString(clientId));
        var grouped = new LinkedHashMap<String, Map<String, Object>>();

        for (var doc : docs) {
            var typeKey = doc.getDocumentTypeId().toString();
            var group = grouped.computeIfAbsent(typeKey, k -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("documentTypeId", k);
                m.put("documents", new ArrayList<Map<String, Object>>());
                return m;
            });
            @SuppressWarnings("unchecked")
            var docList = (List<Map<String, Object>>) group.get("documents");
            docList.add(toSanitizedMap(doc));
        }

        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<Document> listByClient(String clientId) {
        return documentRepository.findByClientId(ClientId.fromString(clientId));
    }

    @Override
    public Document getDocument(String id) {
        return documentRepository.findById(DocumentId.fromString(id))
            .orElseThrow(() -> new NotFoundException("Document", id));
    }

    @Override
    public List<Document> getDocumentGroup(String groupId) {
        return documentRepository.findByDocumentGroupId(DocumentGroupId.fromString(groupId));
    }

    @Override
    public FileDownload download(String id) {
        var doc = getDocument(id);
        if (doc.getEncryptedData() == null) {
            throw new ValidationException("Document has no image data");
        }
        var decrypted = encryptionService.decryptDocument(doc.getEncryptedData());
        return new FileDownload(decrypted, doc.getMimeType(), doc.getOriginalFilename());
    }

    @Override
    public Document replaceFile(String id, byte[] fileData, String originalFilename,
                                String mimeType, int fileSize, String userId) {
        var doc = getDocument(id);
        var processResult = imageProcessingService.processUpload(fileData, mimeType, originalFilename);
        var encrypted = encryptionService.encryptDocument(processResult.data());

        doc.updateEncryptedData(encrypted);
        doc.updateFileInfo(originalFilename, processResult.data().length, mimeType);
        doc.updateMetadata(processResult.metadata().toString());
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public Document updateMetadata(String id, Map<String, Object> metadata, String userId) {
        var doc = getDocument(id);
        if (metadata.containsKey("documentTypeId")) {
            doc.updateType(DocumentTypeId.fromString((String) metadata.get("documentTypeId")));
        }
        if (metadata.containsKey("issueDate") && metadata.get("issueDate") != null) {
            var issueDate = LocalDate.parse((String) metadata.get("issueDate"));
            var expiryDate = metadata.containsKey("expiryDate") && metadata.get("expiryDate") != null
                ? LocalDate.parse((String) metadata.get("expiryDate"))
                : doc.getExpiryDate();
            doc.updateDates(issueDate, expiryDate);
        }
        if (metadata.containsKey("expiryDate") && metadata.get("expiryDate") != null) {
            doc.updateDates(doc.getIssueDate(), LocalDate.parse((String) metadata.get("expiryDate")));
        }
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public void softDelete(String id, String userId) {
        var doc = getDocument(id);
        doc.softDelete(userId != null ? UserId.fromString(userId).value() : null);
        documentRepository.save(doc);
    }

    @Override
    public Document clearImage(String id, String userId) {
        var doc = getDocument(id);
        doc.clearImageData();
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public Document rotate(String id, int angle, String userId) {
        var doc = getDocument(id);
        if (doc.getEncryptedData() == null) {
            throw new ValidationException("Document has no image data");
        }
        var decrypted = encryptionService.decryptDocument(doc.getEncryptedData());
        var rotated = imageProcessingService.rotate(decrypted, angle);
        var encrypted = encryptionService.encryptDocument(rotated);
        doc.updateEncryptedData(encrypted);
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public Document crop(String id, int left, int top, int width, int height, String userId) {
        var doc = getDocument(id);
        if (doc.getEncryptedData() == null) {
            throw new ValidationException("Document has no image data");
        }
        var decrypted = encryptionService.decryptDocument(doc.getEncryptedData());
        var cropped = imageProcessingService.crop(decrypted, left, top, width, height);
        var encrypted = encryptionService.encryptDocument(cropped);
        doc.updateEncryptedData(encrypted);
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public Document optimize(String id, String userId) {
        var doc = getDocument(id);
        if (doc.getEncryptedData() == null) {
            throw new ValidationException("Document has no image data");
        }
        var decrypted = encryptionService.decryptDocument(doc.getEncryptedData());
        var optimized = imageProcessingService.optimize(decrypted, doc.getMimeType());
        var encrypted = encryptionService.encryptDocument(optimized.data());
        doc.updateEncryptedData(encrypted);
        doc.updateFileInfo(doc.getOriginalFilename(), optimized.data().length, doc.getMimeType());
        doc.updateMetadata(optimized.metadata().toString());
        doc.setUpdatedBy(userId != null ? UserId.fromString(userId) : null);
        return documentRepository.save(doc);
    }

    @Override
    public Map<String, Object> checkNumber(String documentNumber) {
        var exists = documentRepository.findByDocumentNumber(documentNumber);
        var result = new HashMap<String, Object>();
        result.put("exists", exists.isPresent());
        exists.ifPresent(doc -> {
            result.put("clientId", doc.getClientId().toString());
        });
        return result;
    }

    @Override
    public Map<String, Object> getAuditMetadata(String id) {
        var doc = getDocument(id);
        var result = new HashMap<String, Object>();
        result.put("createdAt", doc.getCreatedAt());
        result.put("updatedAt", doc.getUpdatedAt());
        result.put("createdBy", doc.getCreatedBy() != null ? doc.getCreatedBy().toString() : null);
        result.put("updatedBy", doc.getUpdatedBy() != null ? doc.getUpdatedBy().toString() : null);
        return result;
    }

    @Override
    public String decryptNumber(String id) {
        var doc = getDocument(id);
        if (doc.getEncryptedDocumentNumber() == null) {
            throw new ValidationException("No encrypted document number stored");
        }
        return encryptionService.decryptDocumentNumber(doc.getEncryptedDocumentNumber());
    }

    private Map<String, Object> toSanitizedMap(Document doc) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", doc.getId());
        m.put("clientId", doc.getClientId().value());
        m.put("documentTypeId", doc.getDocumentTypeId().value());
        m.put("documentNumber", doc.getDocumentNumber());
        m.put("side", doc.getSide().getValue());
        m.put("documentGroupId", doc.getDocumentGroupId() != null ? doc.getDocumentGroupId().value() : null);
        m.put("issueDate", doc.getIssueDate());
        m.put("expiryDate", doc.getExpiryDate());
        m.put("originalFilename", doc.getOriginalFilename());
        m.put("fileSize", doc.getFileSize());
        m.put("mimeType", doc.getMimeType());
        m.put("metadata", doc.getMetadata());
        m.put("createdAt", doc.getCreatedAt());
        m.put("updatedAt", doc.getUpdatedAt());
        return m;
    }
}
