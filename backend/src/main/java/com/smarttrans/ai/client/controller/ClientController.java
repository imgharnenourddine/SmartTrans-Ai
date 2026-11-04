package com.smarttrans.ai.client.controller;

import com.smarttrans.ai.client.dto.ClientResponse;
import com.smarttrans.ai.client.dto.CreateClientRequest;
import com.smarttrans.ai.client.dto.UpdateClientRequest;
import com.smarttrans.ai.client.service.ClientService;
import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
@Tag(name = "Clients", description = "Client company management")
public class ClientController {

	private final ClientService clientService;

	@Operation(summary = "Create a client")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Client created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
	}

	@Operation(summary = "Get client by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Client found"),
			@ApiResponse(responseCode = "404", description = "Client not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ClientResponse getById(
			@Parameter(description = "Client identifier", required = true) @PathVariable UUID id) {
		return clientService.getById(id);
	}

	@Operation(summary = "List all clients")
	@ApiResponse(responseCode = "200", description = "Clients returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ClientResponse> getAll() {
		return clientService.getAll();
	}

	@Operation(summary = "Update a client", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Client updated"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Client not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ClientResponse update(
			@Parameter(description = "Client identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateClientRequest request) {
		return clientService.update(id, request);
	}

	@Operation(summary = "Delete a client")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Client deleted"),
			@ApiResponse(responseCode = "404", description = "Client not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "Client identifier", required = true) @PathVariable UUID id) {
		clientService.delete(id);
	}
}
