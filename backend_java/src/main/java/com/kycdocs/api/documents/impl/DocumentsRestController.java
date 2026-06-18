package com.kycdocs.api.documents.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.api.documents.DocumentsApi;
import com.kycdocs.application.documents.DocumentUseCase;
import com.kycdocs.application.documents.dto.CropImageCommand;
import com.kycdocs.application.documents.dto.RotateImageCommand;
import com.kycdocs.application.documents.dto.UpdateDocumentMetadataCommand;
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
                                                                 String documentTypeId, String side,
                                                                 String documentNumber, String documentGroupId,
                                                                 String issueDate, String expiryDate) {
        try {
            var doc = documentUseCase.upload(
                clientId, documentTypeId,
                documentNumber != null ? documentNumber : "UNKNOWN",
                side,
                file.getBytes(), file.getOriginalFilename(), file.getContentType(),
                (int) file.getSize(), documentGroupId, issueDate, expiryDate, null
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
    public ResponseEntity<ApiResponse<Document>> updateMetadata(String id, UpdateDocumentMetadataCommand command) {
        var metadata = new java.util.HashMap<String, Object>();
        if (command.documentTypeId() != null) metadata.put("documentTypeId", command.documentTypeId());
        if (command.issueDate() != null) metadata.put("issueDate", command.issueDate());
        if (command.expiryDate() != null) metadata.put("expiryDate", command.expiryDate());
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.updateMetadata(id, metadata, null)));
    }

    @Override
    @AuditAction(entityType = "document", action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(String id) {
        documentUseCase.softDelete(id, null);
        return ResponseEntity.ok(ApiResponse.message("Document deleted"));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> clearImage(String id) {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.clearImage(id, null)));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> rotateImage(String id, RotateImageCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.rotate(id, command.angle(), null)
        ));
    }

    @Override
    @AuditAction(entityType = "document", action = "UPDATE")
    public ResponseEntity<ApiResponse<Document>> cropImage(String id, CropImageCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.crop(id, command.left(), command.top(),
                command.width(), command.height(), null)
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
