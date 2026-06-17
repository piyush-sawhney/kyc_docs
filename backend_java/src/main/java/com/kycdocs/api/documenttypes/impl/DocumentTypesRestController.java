package com.kycdocs.api.documenttypes.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.documenttypes.DocumentTypesApi;
import com.kycdocs.application.documenttypes.DocumentTypesUseCase;
import com.kycdocs.domain.documenttype.DocumentType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DocumentTypesRestController implements DocumentTypesApi {

    private final DocumentTypesUseCase documentTypesUseCase;

    public DocumentTypesRestController(DocumentTypesUseCase documentTypesUseCase) {
        this.documentTypesUseCase = documentTypesUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<List<DocumentType>>> listDocumentTypes() {
        return ResponseEntity.ok(ApiResponse.ok(documentTypesUseCase.listAll()));
    }

    @Override
    public ResponseEntity<ApiResponse<DocumentType>> createDocumentType(Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentTypesUseCase.create(body.get("name"))
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(String id, Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentTypesUseCase.update(id, body.get("name"))
        ));
    }
}
