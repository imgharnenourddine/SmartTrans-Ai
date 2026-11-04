package com.smarttrans.ai.assistant.controller;

import com.smarttrans.ai.assistant.dto.AssistantAskRequest;
import com.smarttrans.ai.assistant.dto.AssistantAskResponse;
import com.smarttrans.ai.assistant.service.AssistantService;
import com.smarttrans.ai.exception.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TRANSPORT_MANAGER')")
@Tag(name = "Assistant", description = "Grounded operational assistant")
public class AssistantController {

	private final AssistantService assistantService;

	@Operation(summary = "Ask the operations assistant")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Answer returned"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	@PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public AssistantAskResponse ask(@Valid @RequestBody AssistantAskRequest request) {
		return assistantService.ask(request);
	}
}
