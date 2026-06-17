package com.kycdocs.api.documents.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.api.documents.DocumentsApi;
import com.kycdocs.application.documents.DocumentUseCase;
import com.kycdocs.domain.document.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class DocumentsRestController implements DocumentsApi {

    private final DocumentUseCase documentUseCase;

    public DocumentsRestController(DocumentUseCase documentUseCase) {
        this.documentUseCase = documentUseCase;
    }

    @Override
    @AuditAction(entityType = "document", action = "CREATE")
    public ResponseEntity<ApiResponse<Document>> uploadDocument(String clientId, MultipartFile file,
                                                                 String documentTypeId, String side) {
        try {
            var doc = documentUseCase.upload(
                clientId, documentTypeId, "UNKNOWN", side,
                file.getBytes(), file.getOriginalFilename(), file.getContentType(),
                (int) file.getSize(), null, null, null, null
            );
            return ResponseEntity.ok(ApiResponse.ok(doc));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getGroupedDocuments(String clientId) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.findGroupedByClient(clientId)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<Document>>> listDocuments(String clientId) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.listByClient(clientId)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<Document>>> getDocumentGroup(String groupId) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.getDocumentGroup(groupId)));
    }

    @Override
    public ResponseEntity<?> downloadDocument(String id) {
        var download = documentUseCase.download(id);
        var mediaType = download.mimeType() != null
            ? MediaType.parseMediaType(download.mimeType())
            : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + download.originalFilename() + "\"")
            .body(download.data());
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> replaceDocument(String id, MultipartFile file) {
        try {
            var doc = documentUseCase.replaceFile(
                id, file.getBytes(), file.getOriginalFilename(),
                file.getContentType(), (int) file.getSize(), null
            );
            return ResponseEntity.ok(ApiResponse.ok(doc));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> updateMetadata(String id, Map<String, Object> metadata) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.updateMetadata(id, metadata, null)));
    }

    @Override
    @AuditAction(entityType = "document", action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(String id) {
        documentUseCase.softDelete(id, null);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted"));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> clearImage(String id) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.clearImage(id, null)));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> rotateImage(String id, Map<String, Integer> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.rotate(id, body.getOrDefault("angle", 0), null)
        ));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> cropImage(String id, Map<String, Integer> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.crop(id,
                body.getOrDefault("left", 0), body.getOrDefault("top", 0),
                body.getOrDefault("width", 100), body.getOrDefault("height", 100), null)
        ));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> optimizeImage(String id) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.optimize(id, null)));
    }

    @Override
    public ResponseEntity<ApiResponse<Boolean>> checkDocumentNumber(String number) {
        return ResponseEntity.ok(ApiResponse.ok(
            (Boolean) documentUseCase.checkNumber(number).get("exists")
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDocumentMetadata(String id) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.getAuditMetadata(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<String>> decryptDocumentNumber(String id) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.decryptNumber(id)));
    }
}
