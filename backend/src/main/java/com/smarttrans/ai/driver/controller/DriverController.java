package com.smarttrans.ai.driver.controller;

import com.smarttrans.ai.driver.dto.CreateDriverRequest;
import com.smarttrans.ai.driver.dto.DriverResponse;
import com.smarttrans.ai.driver.dto.UpdateDriverAvailabilityRequest;
import com.smarttrans.ai.driver.dto.UpdateDriverRequest;
import com.smarttrans.ai.driver.enums.DriverStatus;
import com.smarttrans.ai.driver.service.DriverService;
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
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver management")
public class DriverController {

	private final DriverService driverService;

	@Operation(summary = "Create a driver")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Driver created"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "License or user already linked",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Linked user not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
	}

	@Operation(summary = "Get current driver profile")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Driver profile returned"),
			@ApiResponse(responseCode = "404", description = "Driver profile not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('DRIVER')")
	public DriverResponse getMe() {
		return driverService.getMe();
	}

	@Operation(summary = "List drivers with expiring licenses")
	@ApiResponse(responseCode = "200", description = "Drivers with expiring licenses returned")
	@GetMapping(value = "/expiring-licenses", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public List<DriverResponse> getExpiringLicenses(
			@Parameter(description = "Number of days ahead to check", example = "30")
			@RequestParam(defaultValue = "30") int withinDays) {
		return driverService.findExpiringLicenses(withinDays);
	}

	@Operation(summary = "Search and filter drivers")
	@ApiResponse(responseCode = "200", description = "Drivers returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public List<DriverResponse> search(
			@Parameter(description = "Search on name, phone or license number")
			@RequestParam(required = false) String search,
			@Parameter(description = "Filter by operational status")
			@RequestParam(required = false) DriverStatus status,
			@Parameter(description = "Filter by license expiration state")
			@RequestParam(required = false) Boolean licenseExpired,
			@Parameter(description = "Only drivers available for mission")
			@RequestParam(required = false) Boolean availableOnly) {
		return driverService.search(search, status, licenseExpired, availableOnly);
	}

	@Operation(summary = "Get driver by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Driver found"),
			@ApiResponse(responseCode = "403", description = "Access denied",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Driver not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	public DriverResponse getById(
			@Parameter(description = "Driver identifier", required = true) @PathVariable UUID id) {
		return driverService.getById(id);
	}

	@Operation(summary = "Update a driver", description = "Partial update: only non-null fields are applied.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Driver updated"),
			@ApiResponse(responseCode = "404", description = "Driver not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "License or user conflict",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public DriverResponse update(
			@Parameter(description = "Driver identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateDriverRequest request) {
		return driverService.update(id, request);
	}

	@Operation(summary = "Update driver availability")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Availability updated"),
			@ApiResponse(responseCode = "403", description = "Access denied",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Driver on mission or suspended",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Invalid availability change",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}/availability", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER', 'DRIVER')")
	public DriverResponse updateAvailability(
			@Parameter(description = "Driver identifier", required = true) @PathVariable UUID id,
			@Valid @RequestBody UpdateDriverAvailabilityRequest request) {
		return driverService.updateAvailability(id, request);
	}

	@Operation(summary = "Deactivate a driver")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Driver deactivated"),
			@ApiResponse(responseCode = "404", description = "Driver not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Driver on mission",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PatchMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public DriverResponse deactivate(
			@Parameter(description = "Driver identifier", required = true) @PathVariable UUID id) {
		return driverService.deactivate(id);
	}

	@Operation(summary = "Deactivate a driver (alias)", description = "Soft delete: sets status to SUSPENDED.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Driver deactivated"),
			@ApiResponse(responseCode = "404", description = "Driver not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Driver on mission",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
	public void delete(
			@Parameter(description = "Driver identifier", required = true) @PathVariable UUID id) {
		driverService.delete(id);
	}
}
