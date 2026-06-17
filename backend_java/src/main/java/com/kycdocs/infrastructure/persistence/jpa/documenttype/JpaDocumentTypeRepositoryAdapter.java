package com.kycdocs.infrastructure.persistence.jpa.documenttype;

import com.kycdocs.domain.documenttype.DocumentType;
import com.kycdocs.domain.documenttype.DocumentTypeId;
import com.kycdocs.domain.documenttype.DocumentTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaDocumentTypeRepositoryAdapter implements DocumentTypeRepository {

    private final SpringDataDocumentTypeRepository springRepo;
    private final DocumentTypeJpaMapper mapper;

    public JpaDocumentTypeRepositoryAdapter(SpringDataDocumentTypeRepository springRepo, DocumentTypeJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<DocumentType> findById(DocumentTypeId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<DocumentType> findByName(String name) {
        return springRepo.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<DocumentType> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public DocumentType save(DocumentType documentType) {
        var saved = springRepo.save(mapper.toJpa(documentType));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(DocumentTypeId id) {
        springRepo.deleteById(id.value());
    }
}
