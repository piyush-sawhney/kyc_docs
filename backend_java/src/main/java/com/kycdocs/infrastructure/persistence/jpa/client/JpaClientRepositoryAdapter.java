package com.kycdocs.infrastructure.persistence.jpa.client;

import com.kycdocs.domain.client.Client;
import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.client.ClientRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaClientRepositoryAdapter implements ClientRepository {

    private final SpringDataClientRepository springRepo;
    private final ClientJpaMapper mapper;

    public JpaClientRepositoryAdapter(SpringDataClientRepository springRepo, ClientJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Client> findById(ClientId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Client> findAllActive() {
        return springRepo.findAllActive().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Client> findAllDeleted() {
        return springRepo.findAllDeleted().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Client save(Client client) {
        var saved = springRepo.save(mapper.toJpa(client));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(ClientId id) {
        springRepo.deleteById(id.value());
    }
}
