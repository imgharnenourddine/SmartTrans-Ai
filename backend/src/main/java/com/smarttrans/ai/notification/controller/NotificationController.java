package com.smarttrans.ai.notification.controller;

import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import com.smarttrans.ai.notification.dto.CreateNotificationRequest;
import com.smarttrans.ai.notification.dto.NotificationResponse;
import com.smarttrans.ai.notification.dto.UpdateNotificationRequest;
import com.smarttrans.ai.notification.service.NotificationService;
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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

	private final NotificationService notificationService;

	@Operation(summary = "Create a notification")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Notification created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Related resource not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(request));
	}

	@Operation(summary = "Get notification by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Notification found"),
			@ApiResponse(responseCode = "404", description = "Notification not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public NotificationResponse getById(
			@Parameter(description = "Notification identifier", required = true) @PathVariable UUID id) {
		return notificationService.getById(id);
	}

	@Operation(summary = "List all notifications")
	@ApiResponse(responseCode = "200", description = "Notifications returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<NotificationResponse> getAll() {
		return notificationService.getAll();
	}

	@Operation(summary = "Update a notification", description = "Partial update. Use markAsRead=true to set readAt.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Notification updated"),
			@ApiResponse(responseCode = "404", description = "Notification not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public NotificationResponse update(
			@Parameter(description = "Notification identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateNotificationRequest request) {
		return notificationService.update(id, request);
	}

	@Operation(summary = "Delete a notification")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Notification deleted"),
			@ApiResponse(responseCode = "404", description = "Notification not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "Notification identifier", required = true) @PathVariable UUID id) {
		notificationService.delete(id);
	}
}
