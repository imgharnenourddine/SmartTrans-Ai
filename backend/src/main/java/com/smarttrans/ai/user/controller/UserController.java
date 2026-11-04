package com.smarttrans.ai.user.controller;

import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import com.smarttrans.ai.user.dto.CreateUserRequest;
import com.smarttrans.ai.user.dto.UpdateUserRequest;
import com.smarttrans.ai.user.dto.UserResponse;
import com.smarttrans.ai.user.service.UserService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "User account management")
public class UserController {

	private final UserService userService;

	@Operation(summary = "Create a user", description = "Creates a new user with one or more roles. Password is hashed before persistence.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Email already in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Business rule violation (e.g. unknown role)",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
	}

	@Operation(summary = "Get user by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User found"),
			@ApiResponse(responseCode = "404", description = "User not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public UserResponse getById(
			@Parameter(description = "User identifier", required = true) @PathVariable UUID id) {
		return userService.getById(id);
	}

	@Operation(summary = "List all users")
	@ApiResponse(responseCode = "200", description = "Users returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<UserResponse> getAll() {
		return userService.getAll();
	}

	@Operation(summary = "Update a user", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User updated"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "User not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Email already in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public UserResponse update(
			@Parameter(description = "User identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateUserRequest request) {
		return userService.update(id, request);
	}

	@Operation(summary = "Delete a user")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "User deleted"),
			@ApiResponse(responseCode = "404", description = "User not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "User identifier", required = true) @PathVariable UUID id) {
		userService.delete(id);
	}
}
