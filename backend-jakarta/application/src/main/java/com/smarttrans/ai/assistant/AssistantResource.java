package com.smarttrans.ai.assistant;

import com.smarttrans.ai.assistant.AssistantService.AskRequest;
import com.smarttrans.ai.assistant.AssistantService.AskResponse;
import com.smarttrans.ai.security.Access;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/assistant")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AssistantResource {

    @Inject
    private AssistantService assistant;

    @Inject
    private Access access;

    @POST
    @Path("/ask")
    @Consumes(MediaType.APPLICATION_JSON)
    public AskResponse ask(@Valid AskRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return assistant.ask(request);
    }
}
