package com.kycdocs.api.clients.impl;

import com.kycdocs.api.clients.ClientsApi;
import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.application.clients.ClientsUseCase;
import com.kycdocs.domain.client.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ClientsRestController implements ClientsApi {

    private final ClientsUseCase clientsUseCase;

    public ClientsRestController(ClientsUseCase clientsUseCase) {
        this.clientsUseCase = clientsUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<List<Client>>> listActiveClients() {
        return ResponseEntity.ok(ApiResponse.ok(clientsUseCase.listActiveClients()));
    }

    @Override
    public ResponseEntity<ApiResponse<List<Client>>> listDeletedClients() {
        return ResponseEntity.ok(ApiResponse.ok(clientsUseCase.listDeletedClients()));
    }

    @Override
    @AuditAction(entityType = "client", action = "CREATE")
    public ResponseEntity<ApiResponse<Client>> createClient(Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            clientsUseCase.createClient(body.get("name"), body.get("createdBy"))
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> getClient(String id) {
        return ResponseEntity.ok(ApiResponse.ok(clientsUseCase.getClient(id)));
    }

    @Override
    @AuditAction(entityType = "client", action = "UPDATE")
    public ResponseEntity<ApiResponse<Client>> updateClient(String id, Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            clientsUseCase.updateClient(id, body.get("name"), body.get("avatar"))
        ));
    }

    @Override
    @AuditAction(entityType = "client", action = "UPDATE")
    public ResponseEntity<ApiResponse<Client>> patchClient(String id, Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            clientsUseCase.updateClient(id, body.get("name"), body.get("avatar"))
        ));
    }

    @Override
    @AuditAction(entityType = "client", action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteClient(String id) {
        clientsUseCase.softDeleteClient(id, null);
        return ResponseEntity.ok(ApiResponse.ok("Client deleted"));
    }

    @Override
    @AuditAction(entityType = "client", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> restoreClient(String id) {
        clientsUseCase.restoreClient(id);
        return ResponseEntity.ok(ApiResponse.ok("Client restored"));
    }

    @Override
    @AuditAction(entityType = "client", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> mergeClients(String id, Map<String, String> body) {
        clientsUseCase.mergeClients(id, body.get("targetClientId"));
        return ResponseEntity.ok(ApiResponse.ok("Clients merged"));
    }
}
