package com.kycdocs.application.documenttypes.impl;

import com.kycdocs.application.documenttypes.DocumentTypesUseCase;
import com.kycdocs.domain.documenttype.DocumentType;
import com.kycdocs.domain.documenttype.DocumentTypeId;
import com.kycdocs.domain.documenttype.DocumentTypeRepository;
import com.kycdocs.shared.exception.NotFoundException;

import java.util.List;

public class DocumentTypesUseCaseImpl implements DocumentTypesUseCase {

    private final DocumentTypeRepository documentTypeRepository;

    private static final List<String> DEFAULT_DOCUMENT_TYPES = List.of(
        "PAN", "Aadhar", "Passport", "Driving License", "Voter ID", "OCI"
    );

    public DocumentTypesUseCaseImpl(DocumentTypeRepository documentTypeRepository) {
        this.documentTypeRepository = documentTypeRepository;
    }

    @Override
    public List<DocumentType> listAll() {
        autoSeedDefaults();
        return documentTypeRepository.findAll();
    }

    @Override
    public DocumentType create(String name) {
        var docType = DocumentType.create(name);
        return documentTypeRepository.save(docType);
    }

    @Override
    public DocumentType update(String id, String name) {
        var docType = documentTypeRepository.findById(DocumentTypeId.fromString(id))
            .orElseThrow(() -> new NotFoundException("DocumentType", id));
        docType.updateName(name);
        return documentTypeRepository.save(docType);
    }

    private void autoSeedDefaults() {
        for (var name : DEFAULT_DOCUMENT_TYPES) {
            if (documentTypeRepository.findByName(name).isEmpty()) {
                documentTypeRepository.save(DocumentType.create(name));
            }
        }
    }
}
