package com.smarttrans.ai.mission.controller;

import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import com.smarttrans.ai.mission.dto.CreateMissionRequest;
import com.smarttrans.ai.mission.dto.MissionResponse;
import com.smarttrans.ai.mission.dto.MissionStatusUpdateRequest;
import com.smarttrans.ai.mission.dto.UpdateMissionRequest;
import com.smarttrans.ai.mission.service.MissionService;
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
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
@Tag(name = "Missions", description = "Transport mission management")
public class MissionController {

	private final MissionService missionService;

	@Operation(summary = "Create a mission")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Mission created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Reference already in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Related resource not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MissionResponse> create(@Valid @RequestBody CreateMissionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(missionService.create(request));
	}

	@Operation(summary = "Get mission by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Mission found"),
			@ApiResponse(responseCode = "404", description = "Mission not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public MissionResponse getById(
			@Parameter(description = "Mission identifier", required = true) @PathVariable UUID id) {
		return missionService.getById(id);
	}

	@Operation(summary = "List all missions")
	@ApiResponse(responseCode = "200", description = "Missions returned")
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<MissionResponse> getAll() {
		return missionService.search(null, null, null, null, null, null, null);
	}

	@Operation(summary = "Update a mission", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Mission updated"),
			@ApiResponse(responseCode = "404", description = "Mission not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Reference already in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public MissionResponse update(
			@Parameter(description = "Mission identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateMissionRequest request) {
		return missionService.update(id, request);
	}

	@Operation(summary = "Update mission status")
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	@PatchMapping(
			value = "/{id}/status",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public MissionResponse updateStatus(
			@Parameter(description = "Mission identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody MissionStatusUpdateRequest request) {
		return missionService.updateStatus(id, request);
	}

	@Operation(summary = "Delete a mission")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Mission deleted"),
			@ApiResponse(responseCode = "404", description = "Mission not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "Mission identifier", required = true) @PathVariable UUID id) {
		missionService.delete(id);
	}
}
