package com.smarttrans.ai.role.controller;

import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import com.smarttrans.ai.role.dto.CreateRoleRequest;
import com.smarttrans.ai.role.dto.RoleResponse;
import com.smarttrans.ai.role.dto.UpdateRoleRequest;
import com.smarttrans.ai.role.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Roles", description = "Role catalog management")
public class RoleController {

	private final RoleService roleService;

	@Operation(summary = "Create a role")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Role created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Role code already exists",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
	}

	@Operation(summary = "Get role by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Role found"),
			@ApiResponse(responseCode = "404", description = "Role not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public RoleResponse getById(
			@Parameter(description = "Role identifier", required = true) @PathVariable UUID id) {
		return roleService.getById(id);
	}

	@Operation(summary = "List all roles")
	@ApiResponse(responseCode = "200", description = "Roles returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<RoleResponse> getAll() {
		return roleService.getAll();
	}

	@Operation(summary = "Update a role", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Role updated"),
			@ApiResponse(responseCode = "404", description = "Role not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Role code already exists",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public RoleResponse update(
			@Parameter(description = "Role identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateRoleRequest request) {
		return roleService.update(id, request);
	}

	@Operation(summary = "Delete a role")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Role deleted"),
			@ApiResponse(responseCode = "404", description = "Role not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "Role identifier", required = true) @PathVariable UUID id) {
		roleService.delete(id);
	}
}
