package com.kycdocs.api.documenttypes;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.domain.documenttype.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Document Types")
@RequestMapping("/api/document-types")
public interface DocumentTypesApi {

    @Operation(summary = "List all document types")
    @GetMapping
    ResponseEntity<ApiResponse<List<DocumentType>>> listDocumentTypes();

    @Operation(summary = "Create document type")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<DocumentType>> createDocumentType(@RequestBody Map<String, String> body);

    @Operation(summary = "Update document type")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(@PathVariable String id,
                                                                  @RequestBody Map<String, String> body);
}
