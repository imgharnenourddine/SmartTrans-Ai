package com.smarttrans.ai.client;

import com.smarttrans.ai.client.ClientService.ClientResponse;
import com.smarttrans.ai.client.ClientService.CreateClientRequest;
import com.smarttrans.ai.client.ClientService.UpdateClientRequest;
import com.smarttrans.ai.security.Access;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/clients")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ClientResource {

    @Inject
    private ClientService clients;

    @Inject
    private Access access;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateClientRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return Response.status(Response.Status.CREATED).entity(clients.create(request)).build();
    }

    @GET
    @Path("/{id}")
    public ClientResponse getById(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return clients.getById(id);
    }

    @GET
    public List<ClientResponse> getAll() {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return clients.getAll();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ClientResponse update(@PathParam("id") UUID id, @Valid UpdateClientRequest request) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        return clients.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        access.requireAny("ADMIN", "TRANSPORT_MANAGER");
        clients.delete(id);
        return Response.noContent().build();
    }
}
