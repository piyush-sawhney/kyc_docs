package com.kycdocs.domain.documenttype;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository {

    Optional<DocumentType> findById(DocumentTypeId id);

    Optional<DocumentType> findByName(String name);

    List<DocumentType> findAll();

    DocumentType save(DocumentType documentType);

    void delete(DocumentTypeId id);
}
