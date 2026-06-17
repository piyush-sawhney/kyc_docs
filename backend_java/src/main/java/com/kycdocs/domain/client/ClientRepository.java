package com.kycdocs.domain.client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {

    Optional<Client> findById(ClientId id);

    List<Client> findAllActive();

    List<Client> findAllDeleted();

    Client save(Client client);

    void delete(ClientId id);
}
