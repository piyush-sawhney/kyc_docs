package com.kycdocs.api.documenttypes;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.application.documenttypes.dto.CreateDocumentTypeCommand;
import com.kycdocs.application.documenttypes.dto.UpdateDocumentTypeCommand;
import com.kycdocs.domain.documenttype.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Document Types")
@RequestMapping("/api/document-types")
public interface DocumentTypesApi {

    @Operation(summary = "List all document types")
    @GetMapping
    ResponseEntity<ApiResponse<List<DocumentType>>> listDocumentTypes();

    @Operation(summary = "Create document type")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<DocumentType>> createDocumentType(@Valid @RequestBody CreateDocumentTypeCommand command);

    @Operation(summary = "Update document type")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(@PathVariable String id,
                                                                  @Valid @RequestBody UpdateDocumentTypeCommand command);
}
