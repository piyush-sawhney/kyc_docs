package com.kycdocs.application.clients.impl;

import com.kycdocs.application.clients.ClientsUseCase;
import com.kycdocs.domain.client.Client;
import com.kycdocs.domain.client.ClientId;
import com.kycdocs.domain.client.ClientRepository;
import com.kycdocs.domain.document.Document;
import com.kycdocs.domain.document.DocumentRepository;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.shared.exception.NotFoundException;
import com.kycdocs.shared.exception.ValidationException;

import java.util.List;

public class ClientsUseCaseImpl implements ClientsUseCase {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;

    public ClientsUseCaseImpl(ClientRepository clientRepository, DocumentRepository documentRepository) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    public List<Client> listActiveClients() {
        return clientRepository.findAllActive();
    }

    @Override
    public List<Client> listDeletedClients() {
        return clientRepository.findAllDeleted();
    }

    @Override
    public Client getClient(String clientId) {
        return clientRepository.findById(ClientId.fromString(clientId))
            .orElseThrow(() -> new NotFoundException("Client", clientId));
    }

    @Override
    public Client createClient(String name, String createdByUserId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Client name is required");
        }
        var client = Client.create(name, UserId.fromString(createdByUserId));
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(String clientId, String name, String avatar) {
        var client = getClient(clientId);
        if (name != null) client.updateName(name);
        if (avatar != null) client.updateAvatar(avatar);
        return clientRepository.save(client);
    }

    @Override
    public void softDeleteClient(String clientId, String deletedByUserId) {
        var client = getClient(clientId);
        client.softDelete(UserId.fromString(deletedByUserId).value());
        clientRepository.save(client);
    }

    @Override
    public void restoreClient(String clientId) {
        var client = clientRepository.findById(ClientId.fromString(clientId))
            .orElseThrow(() -> new NotFoundException("Client", clientId));
        client.restore();
        clientRepository.save(client);
    }

    @Override
    public void mergeClients(String sourceClientId, String targetClientId) {
        var source = getClient(sourceClientId);
        var target = getClient(targetClientId);

        var documents = documentRepository.findByClientId(ClientId.fromString(sourceClientId));
        for (var doc : documents) {
            documentRepository.save(doc);
        }

        source.softDelete(target.getId());
        clientRepository.save(source);
    }
}
