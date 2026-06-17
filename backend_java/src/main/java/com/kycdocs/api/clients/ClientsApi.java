package com.kycdocs.api.clients;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.domain.client.Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Clients")
@RequestMapping("/api/clients")
public interface ClientsApi {

    @Operation(summary = "List active clients")
    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    ResponseEntity<ApiResponse<List<Client>>> listActiveClients();

    @Operation(summary = "List deleted clients")
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<Client>>> listDeletedClients();

    @Operation(summary = "Create client")
    @PostMapping
    @PreAuthorize("hasAuthority('client:create')")
    ResponseEntity<ApiResponse<Client>> createClient(@RequestBody Map<String, String> body);

    @Operation(summary = "Get client with documents")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('client:read')")
    ResponseEntity<ApiResponse<Client>> getClient(@PathVariable String id);

    @Operation(summary = "Update client")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client:update')")
    ResponseEntity<ApiResponse<Client>> updateClient(@PathVariable String id, @RequestBody Map<String, String> body);

    @Operation(summary = "Partial update client")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('client:update')")
    ResponseEntity<ApiResponse<Client>> patchClient(@PathVariable String id, @RequestBody Map<String, String> body);

    @Operation(summary = "Soft-delete client")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client:delete')")
    ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable String id);

    @Operation(summary = "Restore soft-deleted client")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<Void>> restoreClient(@PathVariable String id);

    @Operation(summary = "Merge source client into target")
    @PostMapping("/{id}/merge")
    @PreAuthorize("hasAuthority('client:merge')")
    ResponseEntity<ApiResponse<Void>> mergeClients(@PathVariable String id, @RequestBody Map<String, String> body);
}
