package com.kycdocs.infrastructure.persistence.jpa.document;

import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.document.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaDocumentRepositoryAdapter implements DocumentRepository {

    private final SpringDataDocumentRepository springRepo;
    private final DocumentJpaMapper mapper;

    public JpaDocumentRepositoryAdapter(SpringDataDocumentRepository springRepo, DocumentJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Document> findByClientId(ClientId clientId) {
        return springRepo.findActiveByClientId(clientId.value()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public List<Document> findByDocumentGroupId(DocumentGroupId groupId) {
        return springRepo.findActiveByDocumentGroupId(groupId.value()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public List<Document> findAllActiveByClientId(ClientId clientId) {
        return springRepo.findAllActiveByClientId(clientId.value()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Document> findByDocumentNumber(String documentNumber) {
        return springRepo.findByDocumentNumber(documentNumber).map(mapper::toDomain);
    }

    @Override
    public Document save(Document document) {
        var saved = springRepo.save(mapper.toJpa(document));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(DocumentId id) {
        springRepo.deleteById(id.value());
    }
}
