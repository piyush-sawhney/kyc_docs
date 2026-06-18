package com.kycdocs.api.documenttypes.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.documenttypes.DocumentTypesApi;
import com.kycdocs.application.documenttypes.DocumentTypesUseCase;
import com.kycdocs.application.documenttypes.dto.CreateDocumentTypeCommand;
import com.kycdocs.application.documenttypes.dto.UpdateDocumentTypeCommand;
import com.kycdocs.domain.documenttype.DocumentType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<ApiResponse<DocumentType>> createDocumentType(CreateDocumentTypeCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentTypesUseCase.create(command.name())
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(String id, UpdateDocumentTypeCommand command) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentTypesUseCase.update(id, command.name())
        ));
    }
}
