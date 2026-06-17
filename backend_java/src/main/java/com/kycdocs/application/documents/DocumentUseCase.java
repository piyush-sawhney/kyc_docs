package com.kycdocs.application.documents;

import com.kycdocs.domain.document.Document;

import java.util.List;
import java.util.Map;

public interface DocumentUseCase {

    Document upload(String clientId, String documentTypeId, String documentNumber, String side,
                    byte[] fileData, String originalFilename, String mimeType, int fileSize,
                    String documentGroupId, String issueDate, String expiryDate, String userId);

    List<Map<String, Object>> findGroupedByClient(String clientId);

    List<Document> listByClient(String clientId);

    Document getDocument(String id);

    List<Document> getDocumentGroup(String groupId);

    Document.FileDownload download(String id);

    Document replaceFile(String id, byte[] fileData, String originalFilename, String mimeType, int fileSize, String userId);

    Document updateMetadata(String id, Map<String, Object> metadata, String userId);

    void softDelete(String id, String userId);

    Document clearImage(String id, String userId);

    Document rotate(String id, int angle, String userId);

    Document crop(String id, int left, int top, int width, int height, String userId);

    Document optimize(String id, String userId);

    Map<String, Object> checkNumber(String documentNumber);

    Map<String, Object> getAuditMetadata(String id);

    String decryptNumber(String id);
}
