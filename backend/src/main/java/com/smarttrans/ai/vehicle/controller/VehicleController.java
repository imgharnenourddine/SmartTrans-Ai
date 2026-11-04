package com.smarttrans.ai.vehicle.controller;

import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import com.smarttrans.ai.vehicle.dto.CreateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.UpdateVehicleRequest;
import com.smarttrans.ai.vehicle.dto.VehicleResponse;
import com.smarttrans.ai.vehicle.enums.VehicleStatus;
import com.smarttrans.ai.vehicle.service.VehicleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Fleet vehicle management")
public class VehicleController {

	private final VehicleService vehicleService;

	@Operation(summary = "Create a vehicle")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Vehicle created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Registration already in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
	}

	@Operation(summary = "List vehicles with expiring insurance or technical visit")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicles with expiring documents returned"),
			@ApiResponse(responseCode = "422", description = "Invalid withinDays value",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/expiring-documents", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public List<VehicleResponse> getWithExpiringDocuments(
			@Parameter(description = "Number of days ahead to check", example = "30")
			@RequestParam(defaultValue = "30") int withinDays) {
		return vehicleService.findWithExpiringDocuments(withinDays);
	}

	@Operation(summary = "Search and filter vehicles")
	@ApiResponse(responseCode = "200", description = "Vehicles returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	public List<VehicleResponse> search(
			@Parameter(description = "Search on registration number")
			@RequestParam(required = false) String search,
			@Parameter(description = "Filter by operational status")
			@RequestParam(required = false) VehicleStatus status,
			@Parameter(description = "Only vehicles available for mission")
			@RequestParam(required = false) Boolean availableOnly,
			@Parameter(description = "Only vehicles whose insurance expires within this number of days")
			@RequestParam(required = false) Integer insuranceExpiringWithinDays,
			@Parameter(description = "Only vehicles whose technical visit expires within this number of days")
			@RequestParam(required = false) Integer technicalVisitExpiringWithinDays) {
		return vehicleService.search(search, status, availableOnly,
				insuranceExpiringWithinDays, technicalVisitExpiringWithinDays);
	}

	@Operation(summary = "Get vehicle by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicle found"),
			@ApiResponse(responseCode = "404", description = "Vehicle not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	public VehicleResponse getById(
			@Parameter(description = "Vehicle identifier", required = true) @PathVariable UUID id) {
		return vehicleService.getById(id);
	}

	@Operation(summary = "Update a vehicle", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicle updated"),
			@ApiResponse(responseCode = "404", description = "Vehicle not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Registration already in use or vehicle in use",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public VehicleResponse update(
			@Parameter(description = "Vehicle identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateVehicleRequest request) {
		return vehicleService.update(id, request);
	}

	@Operation(summary = "Deactivate a vehicle", description = "Soft delete: sets status to OUT_OF_SERVICE.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicle deactivated"),
			@ApiResponse(responseCode = "404", description = "Vehicle not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Vehicle in use on a mission",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public VehicleResponse deactivate(
			@Parameter(description = "Vehicle identifier", required = true) @PathVariable UUID id) {
		return vehicleService.deactivate(id);
	}

	@Operation(summary = "Deactivate a vehicle (alias)", description = "Soft delete: sets status to OUT_OF_SERVICE.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Vehicle deactivated"),
			@ApiResponse(responseCode = "404", description = "Vehicle not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Vehicle in use on a mission",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public void delete(
			@Parameter(description = "Vehicle identifier", required = true) @PathVariable UUID id) {
		vehicleService.delete(id);
	}
}
