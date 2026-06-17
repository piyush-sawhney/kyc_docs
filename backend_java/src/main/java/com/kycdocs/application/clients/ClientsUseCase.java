package com.kycdocs.application.clients;

import com.kycdocs.domain.client.Client;

import java.util.List;

/**
 * Inbound port: client management use cases.
 */
public interface ClientsUseCase {

    List<Client> listActiveClients();

    List<Client> listDeletedClients();

    Client getClient(String clientId);

    Client createClient(String name, String createdByUserId);

    Client updateClient(String clientId, String name, String avatar);

    void softDeleteClient(String clientId, String deletedByUserId);

    void restoreClient(String clientId);

    void mergeClients(String sourceClientId, String targetClientId);
}
