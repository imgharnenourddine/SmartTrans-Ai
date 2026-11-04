package com.smarttrans.ai.activity.controller;

import com.smarttrans.ai.activity.dto.ActivityResponse;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
@Tag(name = "Activities", description = "Read-only audit log")
public class ActivityController {

	private final ActivityService activityService;

	@Operation(summary = "Get activity by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Activity found"),
			@ApiResponse(responseCode = "404", description = "Activity not found",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActivityResponse getById(
			@Parameter(description = "Activity identifier", required = true) @PathVariable UUID id) {
		return activityService.getById(id);
	}

	@Operation(summary = "List all activities")
	@ApiResponse(responseCode = "200", description = "Activities returned")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ActivityResponse> getAll() {
		return activityService.getAll();
	}
}
