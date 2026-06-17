package com.kycdocs.domain.document;

import com.kycdocs.domain.client.ClientId;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    Optional<Document> findById(DocumentId id);

    List<Document> findByClientId(ClientId clientId);

    List<Document> findByDocumentGroupId(DocumentGroupId groupId);

    List<Document> findAllActiveByClientId(ClientId clientId);

    Optional<Document> findByDocumentNumber(String documentNumber);

    Document save(Document document);

    void delete(DocumentId id);
}
