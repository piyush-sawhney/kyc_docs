package com.kycdocs.api.documents;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.application.documents.dto.CropImageCommand;
import com.kycdocs.application.documents.dto.RotateImageCommand;
import com.kycdocs.application.documents.dto.UpdateDocumentMetadataCommand;
import com.kycdocs.domain.document.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Documents")
@RequestMapping("/api")
public interface DocumentsApi {

    @Operation(summary = "Upload document for client")
    @PostMapping("/clients/{clientId}/documents")
    ResponseEntity<ApiResponse<Document>> uploadDocument(@PathVariable String clientId,
                                                           @RequestParam("file") MultipartFile file,
                                                           @RequestParam("documentTypeId") String documentTypeId,
                                                           @RequestParam(value = "side", defaultValue = "front") String side,
                                                           @RequestParam(value = "documentNumber", required = false) String documentNumber,
                                                           @RequestParam(value = "documentGroupId", required = false) String documentGroupId,
                                                           @RequestParam(value = "issueDate", required = false) String issueDate,
                                                           @RequestParam(value = "expiryDate", required = false) String expiryDate);

    @Operation(summary = "Get documents grouped by type")
    @GetMapping("/clients/{clientId}/documents/grouped")
    ResponseEntity<ApiResponse<?>> getGroupedDocuments(@PathVariable String clientId);

    @Operation(summary = "List client's documents")
    @GetMapping("/clients/{clientId}/documents")
    ResponseEntity<ApiResponse<List<Document>>> listDocuments(@PathVariable String clientId);

    @Operation(summary = "Get document group (front + back)")
    @GetMapping("/documents/group/{groupId}")
    ResponseEntity<ApiResponse<List<Document>>> getDocumentGroup(@PathVariable String groupId);

    @Operation(summary = "Download decrypted document")
    @GetMapping("/documents/{id}/download")
    ResponseEntity<?> downloadDocument(@PathVariable String id);

    @Operation(summary = "Re-upload document file")
    @PutMapping("/documents/{id}")
    ResponseEntity<ApiResponse<Document>> replaceDocument(@PathVariable String id,
                                                           @RequestParam("file") MultipartFile file);

    @Operation(summary = "Update document metadata")
    @PatchMapping("/documents/{id}/metadata")
    ResponseEntity<ApiResponse<Document>> updateMetadata(@PathVariable String id,
                                                           @Valid @RequestBody UpdateDocumentMetadataCommand command);

    @Operation(summary = "Soft-delete document")
    @DeleteMapping("/documents/{id}")
    ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable String id);

    @Operation(summary = "Clear document image data")
    @PatchMapping("/documents/{id}/clear-image")
    ResponseEntity<ApiResponse<Document>> clearImage(@PathVariable String id);

    @Operation(summary = "Rotate image")
    @PostMapping("/documents/{id}/rotate")
    ResponseEntity<ApiResponse<Document>> rotateImage(@PathVariable String id,
                                                       @Valid @RequestBody RotateImageCommand command);

    @Operation(summary = "Crop image")
    @PostMapping("/documents/{id}/crop")
    ResponseEntity<ApiResponse<Document>> cropImage(@PathVariable String id,
                                                     @Valid @RequestBody CropImageCommand command);

    @Operation(summary = "Re-optimize image")
    @PostMapping("/documents/{id}/optimize")
    ResponseEntity<ApiResponse<Document>> optimizeImage(@PathVariable String id);

    @Operation(summary = "Check if document number exists")
    @GetMapping("/documents/check-number/{number}")
    ResponseEntity<ApiResponse<Boolean>> checkDocumentNumber(@PathVariable String number);

    @Operation(summary = "Get document metadata")
    @GetMapping("/documents/{id}/metadata")
    ResponseEntity<ApiResponse<?>> getDocumentMetadata(@PathVariable String id);

    @Operation(summary = "Decrypt document number")
    @GetMapping("/documents/{id}/decrypt-number")
    ResponseEntity<ApiResponse<String>> decryptDocumentNumber(@PathVariable String id);
}
